package com.example.data.reciters

import android.content.Context
import com.example.data.local.DownloadedAudioEntity
import com.example.data.local.ReciterCoverageEntity
import com.example.data.local.ReciterDao
import com.example.data.quran.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

class ReciterRepository(
    private val context: Context,
    private val reciterDao: ReciterDao,
    private val quranRepository: QuranRepository,
    private val okHttpClient: OkHttpClient
) {
    fun observeCoverage(reciterId: String): Flow<List<ReciterCoverageEntity>> =
        reciterDao.observeCoverage(reciterId)

    fun observeDownloadedAudio(): Flow<List<DownloadedAudioEntity>> =
        reciterDao.observeDownloadedAudio()

    fun observeTotalAudioBytes(): Flow<Long> =
        reciterDao.observeTotalAudioBytes()

    suspend fun getLocalAudioPath(reciterId: String, surahIndex: Int, ayahNumber: Int): String? =
        withContext(Dispatchers.IO) {
            val entity = reciterDao.getDownloadedAudio(reciterId, surahIndex, ayahNumber)
            if (entity != null && File(entity.localPath).exists()) entity.localPath else null
        }

    /**
     * Verifies real availability of a surah's recordings for a specific reciter via HTTP HEAD
     * and Quran.com timing API, never claiming availability without checking.
     */
    suspend fun verifyAndResolveSurahAudio(
        reciterId: String,
        surahIndex: Int
    ): SurahAudioResolution = withContext(Dispatchers.IO) {
        val reciter = ReciterCatalog.findById(reciterId)
        val surah = quranRepository.getSurah(surahIndex)
        val totalAyahs = surah?.ayahCount ?: 1

        val localFull = getLocalAudioPath(reciterId, surahIndex, 0)

        var fullUrl: String? = null
        val timings = mutableListOf<AyahTimingSegment>()

        if (reciter.quranComChapterReciterId != null) {
            runCatching {
                val apiUrl = "https://api.quran.com/api/v4/chapter_recitations/${reciter.quranComChapterReciterId}/$surahIndex?segments=true"
                val req = Request.Builder().url(apiUrl).header("User-Agent", "WirdQuranApp/1.0").build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val audioFile = JSONObject(body).optJSONObject("audio_file")
                        val candidateUrl = audioFile?.optString("audio_url", "")?.takeIf { it.isNotBlank() }
                        if (candidateUrl != null) {
                            fullUrl = candidateUrl
                            val ts = audioFile.optJSONArray("timestamps")
                            if (ts != null) {
                                for (i in 0 until ts.length()) {
                                    val item = ts.getJSONObject(i)
                                    val key = item.optString("verse_key", "")
                                    val ayahNum = key.substringAfter(":").toIntOrNull() ?: continue
                                    val from = item.optLong("timestamp_from", 0L)
                                    val to = item.optLong("timestamp_to", 0L)
                                    if (to > from) {
                                        timings.add(AyahTimingSegment(surahIndex, ayahNum, from, to))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (fullUrl == null) {
            val mp3Url = reciter.mp3QuranSurahUrl(surahIndex)
            if (mp3Url != null && headExists(mp3Url)) {
                fullUrl = mp3Url
            }
        }

        // Verify ayah-by-ayah recordings (check first and last ayah of the surah)
        val firstAyahUrl = reciter.ayahAudioUrl(surahIndex, 1)
        val lastAyahUrl = reciter.ayahAudioUrl(surahIndex, totalAyahs)
        val ayahAvailable = headExists(firstAyahUrl) && (totalAyahs == 1 || headExists(lastAyahUrl))
        val localFirstAyah = getLocalAudioPath(reciterId, surahIndex, 1) != null

        val verified = ReciterCoverageEntity(
            reciterId = reciterId,
            surahIndex = surahIndex,
            fullSurahAvailable = fullUrl != null || localFull != null,
            ayahAvailableCount = if (ayahAvailable || localFirstAyah) totalAyahs else 0,
            totalAyahCount = totalAyahs,
            hasTimestamps = timings.isNotEmpty(),
            verifiedAt = System.currentTimeMillis()
        )
        reciterDao.upsertCoverage(listOf(verified))

        SurahAudioResolution(
            reciter = reciter,
            surahIndex = surahIndex,
            fullSurahUrl = fullUrl,
            localFullSurahPath = localFull,
            timings = timings,
            ayahUrlsAvailable = ayahAvailable || localFirstAyah,
            verifiedAt = verified.verifiedAt
        )
    }

    suspend fun verifySingleAyahAvailable(reciterId: String, surahIndex: Int, ayahNumber: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (getLocalAudioPath(reciterId, surahIndex, ayahNumber) != null) return@withContext true
            val reciter = ReciterCatalog.findById(reciterId)
            headExists(reciter.ayahAudioUrl(surahIndex, ayahNumber))
        }

    suspend fun verifyAllSurahsCoverage(
        reciterId: String,
        onProgress: (checked: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val reciter = ReciterCatalog.findById(reciterId)
        val surahs = quranRepository.getAllSurahs()
        val batch = mutableListOf<ReciterCoverageEntity>()
        surahs.forEachIndexed { idx, surah ->
            val ayahOk = headExists(reciter.ayahAudioUrl(surah.index, 1))
            val mp3Ok = reciter.mp3QuranSurahUrl(surah.index)?.let { headExists(it) } ?: false
            val quranComSupported = reciter.quranComChapterReciterId != null
            batch.add(
                ReciterCoverageEntity(
                    reciterId = reciterId,
                    surahIndex = surah.index,
                    fullSurahAvailable = mp3Ok || quranComSupported,
                    ayahAvailableCount = if (ayahOk) surah.ayahCount else 0,
                    totalAyahCount = surah.ayahCount,
                    hasTimestamps = quranComSupported,
                    verifiedAt = System.currentTimeMillis()
                )
            )
            if (batch.size >= 10 || idx == surahs.lastIndex) {
                reciterDao.upsertCoverage(batch.toList())
                batch.clear()
            }
            onProgress(idx + 1, surahs.size)
        }
    }

    suspend fun downloadSurahAudio(
        reciterId: String,
        surahIndex: Int,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val resolution = verifyAndResolveSurahAudio(reciterId, surahIndex)
        val url = resolution.fullSurahUrl
            ?: return@withContext Result.failure(IllegalStateException("لا يتوفر ملف سورة كاملة للتحميل لهذا القارئ"))

        runCatching {
            val audioDir = File(context.filesDir, "recitations/$reciterId").apply { mkdirs() }
            val sss = surahIndex.toString().padStart(3, '0')
            val target = File(audioDir, "surah_$sss.mp3")
            val temp = File(audioDir, "surah_$sss.mp3.tmp")

            val req = Request.Builder().url(url).header("User-Agent", "WirdQuranApp/1.0").build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Empty body")
                val total = body.contentLength()
                var readTotal = 0L
                body.byteStream().use { input ->
                    temp.outputStream().use { out ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            out.write(buf, 0, r)
                            readTotal += r
                            onProgress(readTotal, total)
                        }
                    }
                }
            }
            if (target.exists()) target.delete()
            temp.renameTo(target)

            reciterDao.upsertDownloadedAudio(
                DownloadedAudioEntity(
                    reciterId = reciterId,
                    surahIndex = surahIndex,
                    ayahNumber = 0,
                    localPath = target.absolutePath,
                    sizeBytes = target.length()
                )
            )
            target
        }
    }

    suspend fun deleteDownloadedFile(entity: DownloadedAudioEntity) = withContext(Dispatchers.IO) {
        File(entity.localPath).delete()
        reciterDao.deleteDownloadedAudio(entity.reciterId, entity.surahIndex, entity.ayahNumber)
    }

    suspend fun deleteAllAudio() = withContext(Dispatchers.IO) {
        File(context.filesDir, "recitations").deleteRecursively()
        reciterDao.deleteAllAudio()
    }

    private fun headExists(url: String): Boolean {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "WirdQuranApp/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        }.getOrDefault(false)
    }
}
