package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.WirdPreferences
import com.example.data.quran.QuranRepository
import com.example.data.reciters.AyahTimingSegment
import com.example.data.reciters.ReciterCatalog
import com.example.data.reciters.ReciterInfo
import com.example.data.reciters.ReciterRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackUiState(
    val reciter: ReciterInfo = ReciterCatalog.RECITERS.first(),
    val surahIndex: Int = 1,
    val surahName: String = "الفاتحة",
    val currentAyah: Int = 1,
    val totalAyahs: Int = 7,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isVisible: Boolean = false,
    val isFullSurahStream: Boolean = false,
    val hasAyahSyncTimings: Boolean = false,
    val speed: Float = 1.0f,
    val singleAyahRepeatTarget: Int = 1, // 1 = no repeat, 0 = infinite, 3/5 = N times
    val singleAyahRepeatDone: Int = 0,
    val rangeStartAyah: Int? = null,
    val rangeEndAyah: Int? = null,
    val rangeRepeatTarget: Int = 1,
    val rangeRepeatDone: Int = 0,
    val sleepTimerRemainingSec: Int? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class RecitationPlayerManager(
    private val context: Context,
    private val reciterRepository: ReciterRepository,
    private val quranRepository: QuranRepository,
    private val preferences: WirdPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var timings: List<AyahTimingSegment> = emptyList()
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastSavedTick = 0L

    private fun ensureController(onReady: (MediaController) -> Unit) {
        controller?.let {
            onReady(it)
            return
        }
        val token = SessionToken(context, ComponentName(context, WirdPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val ctrl = future.get()
                controller = ctrl
                attachListener(ctrl)
                onReady(ctrl)
            }.onFailure { err ->
                _state.update {
                    it.copy(errorMessage = "تعذر تشغيل مشغل التلاوة: ${err.localizedMessage ?: "خطأ غير معروف"}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun attachListener(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    handlePlaylistOrItemEnded(player)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val ayahTag = mediaItem?.mediaId?.substringAfter(":")?.toIntOrNull()
                if (ayahTag != null && !_state.value.isFullSurahStream) {
                    _state.update { it.copy(currentAyah = ayahTag) }
                    persistListeningPosition()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "تعذر تحميل التلاوة الحقيقية للقارئ ${_state.value.reciter.nameArabic}. تحقق من الاتصال بالإنترنت أو توفر التسجيل."
                    )
                }
            }
        })
    }

    private fun handlePlaylistOrItemEnded(player: MediaController) {
        val st = _state.value
        // Handle single ayah repeat when playing single ayah
        if (!st.isFullSurahStream && st.rangeStartAyah == null && st.singleAyahRepeatTarget != 1) {
            val done = st.singleAyahRepeatDone + 1
            if (st.singleAyahRepeatTarget == 0 || done < st.singleAyahRepeatTarget) {
                _state.update { it.copy(singleAyahRepeatDone = done) }
                player.seekTo(0)
                player.play()
                return
            }
        }
        // Handle range repeat
        if (st.rangeStartAyah != null && st.rangeEndAyah != null) {
            val done = st.rangeRepeatDone + 1
            if (st.rangeRepeatTarget == 0 || done < st.rangeRepeatTarget) {
                _state.update { it.copy(rangeRepeatDone = done) }
                player.seekTo(0, 0L)
                player.play()
                return
            }
        }
        _state.update { it.copy(isPlaying = false, singleAyahRepeatDone = 0, rangeRepeatDone = 0) }
    }

    fun playSingleAyah(reciterId: String, surahIndex: Int, ayahNumber: Int) {
        scope.launch {
            val reciter = ReciterCatalog.findById(reciterId)
            val surah = quranRepository.getSurah(surahIndex)
            _state.update {
                it.copy(
                    reciter = reciter,
                    surahIndex = surahIndex,
                    surahName = surah?.nameArabic ?: "",
                    currentAyah = ayahNumber,
                    totalAyahs = surah?.ayahCount ?: 1,
                    isVisible = true,
                    isFullSurahStream = false,
                    hasAyahSyncTimings = false,
                    rangeStartAyah = null,
                    rangeEndAyah = null,
                    singleAyahRepeatDone = 0,
                    errorMessage = null,
                    isBuffering = true
                )
            }

            val available = reciterRepository.verifySingleAyahAvailable(reciterId, surahIndex, ayahNumber)
            if (!available) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "التلاوة غير متوفرة لهذه الآية بصوت ${reciter.nameArabic} أو لا يوجد اتصال بالإنترنت."
                    )
                }
                return@launch
            }

            val localPath = reciterRepository.getLocalAudioPath(reciterId, surahIndex, ayahNumber)
            val uri = if (localPath != null) Uri.fromFile(File(localPath)) else Uri.parse(reciter.ayahAudioUrl(surahIndex, ayahNumber))
            val item = buildMediaItem("$surahIndex:$ayahNumber", uri, "سورة ${surah?.nameArabic ?: surahIndex} — آية $ayahNumber", reciter.nameArabic)

            ensureController { ctrl ->
                ctrl.setMediaItem(item)
                ctrl.setPlaybackSpeed(_state.value.speed)
                ctrl.prepare()
                ctrl.play()
                persistListeningPosition()
            }
        }
    }

    fun playSurah(reciterId: String, surahIndex: Int, startAyah: Int = 1) {
        scope.launch {
            val reciter = ReciterCatalog.findById(reciterId)
            val surah = quranRepository.getSurah(surahIndex)
            val totalAyahs = surah?.ayahCount ?: 1

            _state.update {
                it.copy(
                    reciter = reciter,
                    surahIndex = surahIndex,
                    surahName = surah?.nameArabic ?: "",
                    currentAyah = startAyah.coerceIn(1, totalAyahs),
                    totalAyahs = totalAyahs,
                    isVisible = true,
                    rangeStartAyah = null,
                    rangeEndAyah = null,
                    singleAyahRepeatDone = 0,
                    errorMessage = null,
                    isBuffering = true
                )
            }

            val resolution = reciterRepository.verifyAndResolveSurahAudio(reciterId, surahIndex)

            // Prefer full-surah stream if local or timing segments exist (and startAyah == 1 or timings exist)
            val fullUri = when {
                resolution.localFullSurahPath != null -> Uri.fromFile(File(resolution.localFullSurahPath))
                resolution.fullSurahUrl != null && (startAyah == 1 || resolution.timings.isNotEmpty()) -> Uri.parse(resolution.fullSurahUrl)
                else -> null
            }

            if (fullUri != null && (resolution.timings.isNotEmpty() || !resolution.ayahUrlsAvailable)) {
                timings = resolution.timings
                val seekPos = timings.firstOrNull { it.ayahNumber == startAyah }?.startMs ?: 0L
                val item = buildMediaItem("$surahIndex:0", fullUri, "سورة ${surah?.nameArabic ?: surahIndex}", reciter.nameArabic)
                _state.update {
                    it.copy(
                        isFullSurahStream = true,
                        hasAyahSyncTimings = timings.isNotEmpty()
                    )
                }
                ensureController { ctrl ->
                    ctrl.setMediaItem(item)
                    ctrl.setPlaybackSpeed(_state.value.speed)
                    ctrl.prepare()
                    if (seekPos > 0) ctrl.seekTo(seekPos)
                    ctrl.play()
                    persistListeningPosition()
                }
                return@launch
            }

            if (!resolution.ayahUrlsAvailable) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "لا تتوفر تسجيلات محققة لهذه السورة بصوت ${reciter.nameArabic} حاليًا (أو لا يوجد اتصال)."
                    )
                }
                return@launch
            }

            // Build ayah-by-ayah Media3 playlist -> guarantees exact ayah highlight, next/prev ayah, and smooth playback
            timings = emptyList()
            val mediaItems = (1..totalAyahs).map { ayahNum ->
                val uri = Uri.parse(reciter.ayahAudioUrl(surahIndex, ayahNum))
                buildMediaItem("$surahIndex:$ayahNum", uri, "سورة ${surah?.nameArabic ?: surahIndex} — آية $ayahNum", reciter.nameArabic)
            }
            _state.update {
                it.copy(
                    isFullSurahStream = false,
                    hasAyahSyncTimings = true
                )
            }
            ensureController { ctrl ->
                ctrl.setMediaItems(mediaItems, (startAyah - 1).coerceIn(0, totalAyahs - 1), 0L)
                ctrl.setPlaybackSpeed(_state.value.speed)
                ctrl.prepare()
                ctrl.play()
                persistListeningPosition()
            }
        }
    }

    fun playAyahRange(reciterId: String, surahIndex: Int, fromAyah: Int, toAyah: Int, repeatTimes: Int) {
        scope.launch {
            val reciter = ReciterCatalog.findById(reciterId)
            val surah = quranRepository.getSurah(surahIndex)
            val maxAyah = surah?.ayahCount ?: 1
            val start = fromAyah.coerceIn(1, maxAyah)
            val end = toAyah.coerceIn(start, maxAyah)

            _state.update {
                it.copy(
                    reciter = reciter,
                    surahIndex = surahIndex,
                    surahName = surah?.nameArabic ?: "",
                    currentAyah = start,
                    totalAyahs = maxAyah,
                    isVisible = true,
                    isFullSurahStream = false,
                    hasAyahSyncTimings = true,
                    rangeStartAyah = start,
                    rangeEndAyah = end,
                    rangeRepeatTarget = repeatTimes,
                    rangeRepeatDone = 0,
                    errorMessage = null,
                    isBuffering = true
                )
            }

            val ok = reciterRepository.verifySingleAyahAvailable(reciterId, surahIndex, start)
            if (!ok) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "تعذر التحقق من توفر الآيات المختارة بصوت ${reciter.nameArabic}."
                    )
                }
                return@launch
            }

            val items = (start..end).map { a ->
                buildMediaItem("$surahIndex:$a", Uri.parse(reciter.ayahAudioUrl(surahIndex, a)), "سورة ${surah?.nameArabic} — آية $a", reciter.nameArabic)
            }
            ensureController { ctrl ->
                ctrl.setMediaItems(items)
                ctrl.setPlaybackSpeed(_state.value.speed)
                ctrl.prepare()
                ctrl.play()
            }
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun nextAyah() {
        val st = _state.value
        val ctrl = controller ?: return
        if (!st.isFullSurahStream) {
            if (ctrl.hasNextMediaItem()) {
                ctrl.seekToNextMediaItem()
            } else if (st.currentAyah < st.totalAyahs) {
                playSingleAyah(st.reciter.id, st.surahIndex, st.currentAyah + 1)
            }
        } else {
            val next = (st.currentAyah + 1).coerceAtMost(st.totalAyahs)
            timings.firstOrNull { it.ayahNumber == next }?.let {
                ctrl.seekTo(it.startMs)
                _state.update { s -> s.copy(currentAyah = next) }
            }
        }
    }

    fun previousAyah() {
        val st = _state.value
        val ctrl = controller ?: return
        if (!st.isFullSurahStream) {
            if (ctrl.hasPreviousMediaItem()) {
                ctrl.seekToPreviousMediaItem()
            } else if (st.currentAyah > 1) {
                playSingleAyah(st.reciter.id, st.surahIndex, st.currentAyah - 1)
            }
        } else {
            val prev = (st.currentAyah - 1).coerceAtLeast(1)
            timings.firstOrNull { it.ayahNumber == prev }?.let {
                ctrl.seekTo(it.startMs)
                _state.update { s -> s.copy(currentAyah = prev) }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        controller?.setPlaybackSpeed(speed)
    }

    fun setSingleAyahRepeat(target: Int) {
        _state.update { it.copy(singleAyahRepeatTarget = target, singleAyahRepeatDone = 0) }
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(sleepTimerRemainingSec = null) }
            return
        }
        var remaining = minutes * 60
        _state.update { it.copy(sleepTimerRemainingSec = remaining) }
        sleepTimerJob = scope.launch {
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _state.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (remaining <= 0) {
                controller?.pause()
                _state.update { it.copy(sleepTimerRemainingSec = null) }
            }
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
    }

    fun stopAndDismiss() {
        controller?.stop()
        stopTicker()
        sleepTimerJob?.cancel()
        _state.update { it.copy(isPlaying = false, isVisible = false, sleepTimerRemainingSec = null) }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.coerceAtLeast(0L)
                    var activeAyah = _state.value.currentAyah
                    if (_state.value.isFullSurahStream && timings.isNotEmpty()) {
                        val matched = timings.firstOrNull { pos >= it.startMs && pos <= it.endMs }
                        if (matched != null) activeAyah = matched.ayahNumber
                    }
                    _state.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            currentAyah = activeAyah
                        )
                    }
                    if (System.currentTimeMillis() - lastSavedTick > 4000L) {
                        lastSavedTick = System.currentTimeMillis()
                        persistListeningPosition()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun persistListeningPosition() {
        val st = _state.value
        scope.launch {
            preferences.saveListeningPosition(
                reciterId = st.reciter.id,
                surahIndex = st.surahIndex,
                ayahNumber = st.currentAyah,
                positionMs = st.currentPositionMs
            )
        }
    }

    private fun buildMediaItem(id: String, uri: Uri, title: String, artist: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
    }

    fun release() {
        stopTicker()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
