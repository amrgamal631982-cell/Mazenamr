package com.example.data.tafsir

import com.example.data.local.TafsirDao
import com.example.data.local.TafsirEntryEntity
import com.example.data.local.WordMeaningsCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class TafsirSourceInfo(
    val id: String,
    val slug: String,
    val titleArabic: String,
    val authorArabic: String,
    val description: String,
    val remoteSourceUrl: String
)

data class TafsirPackStatus(
    val source: TafsirSourceInfo,
    val downloadedSurahs: Int,
    val totalEntries: Int,
    val sizeBytes: Long
)

data class WordMeaningItem(
    val wordUthmani: String,
    val meaning: String,
    val sourceLabel: String
)

data class AyahStudyBundle(
    val surahIndex: Int,
    val ayahNumber: Int,
    val tafsirs: Map<String, String?>, // sourceId -> text or null if not downloaded/missing
    val wordMeanings: List<WordMeaningItem>,
    val wordMeaningsAvailable: Boolean,
    val gharibSummary: String?,
    val sababNuzul: String?,
    val errors: List<String> = emptyList()
)

class TafsirRepository(
    private val tafsirDao: TafsirDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        val SOURCES = listOf(
            TafsirSourceInfo(
                id = "ibn_kathir",
                slug = "ar-tafsir-ibn-kathir",
                titleArabic = "تفسير القرآن العظيم",
                authorArabic = "الحافظ عماد الدين إسماعيل بن كثير",
                description = "تفسير القرآن بالقرآن والسنة وأقوال السلف",
                remoteSourceUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/ar-tafsir-ibn-kathir"
            ),
            TafsirSourceInfo(
                id = "tabari",
                slug = "ar-tafsir-al-tabari",
                titleArabic = "جامع البيان عن تأويل آي القرآن",
                authorArabic = "الإمام أبو جعفر محمد بن جرير الطبري",
                description = "أمّ التفاسير بالمأثور ووجوه اللغة والقراءات",
                remoteSourceUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/ar-tafsir-al-tabari"
            ),
            TafsirSourceInfo(
                id = "saadi",
                slug = "ar-tafseer-al-saddi",
                titleArabic = "تيسير الكريم الرحمن في تفسير كلام المنان",
                authorArabic = "الشيخ عبد الرحمن بن ناصر السعدي",
                description = "تفسير ميسر واضح العبارة يعتني بالمعاني الإيمانية والتربوية",
                remoteSourceUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/ar-tafseer-al-saddi"
            ),
            TafsirSourceInfo(
                id = "gharib",
                slug = "al-muyassar-fi-al-gharib",
                titleArabic = "الميسر في غريب القرآن الكريم",
                authorArabic = "مركز الدراسات والمعلومات القرآنية / مجمع الملك فهد",
                description = "بيان معاني المفردات والكلمات القرآنية الغريبة بإيجاز موثق",
                remoteSourceUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/al-muyassar-fi-al-gharib"
            )
        )
    }

    suspend fun getPackStatuses(): List<TafsirPackStatus> = withContext(Dispatchers.IO) {
        SOURCES.map { src ->
            TafsirPackStatus(
                source = src,
                downloadedSurahs = tafsirDao.countSurahsForSource(src.id),
                totalEntries = tafsirDao.countEntriesForSource(src.id),
                sizeBytes = tafsirDao.estimateBytesForSource(src.id)
            )
        }
    }

    suspend fun getWordMeaningsCacheBytes(): Long = withContext(Dispatchers.IO) {
        tafsirDao.estimateWordMeaningsBytes()
    }

    suspend fun clearWordMeaningsCache() = withContext(Dispatchers.IO) {
        tafsirDao.clearWordMeanings()
    }

    suspend fun deletePack(sourceId: String) = withContext(Dispatchers.IO) {
        tafsirDao.deleteSource(sourceId)
    }

    /**
     * Loads study details for a single ayah from local Room cache first,
     * and if online & missing, optionally fetches that surah's verified JSON on-demand.
     */
    suspend fun loadAyahStudy(
        surahIndex: Int,
        ayahNumber: Int,
        fetchRemoteIfMissing: Boolean
    ): AyahStudyBundle = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val tafsirs = mutableMapOf<String, String?>()

        for (src in SOURCES) {
            var entry = tafsirDao.getEntry(src.id, surahIndex, ayahNumber)
            if (entry == null && fetchRemoteIfMissing) {
                runCatching { fetchAndCacheSurahTafsir(src, surahIndex) }
                    .onFailure { errors.add("${src.titleArabic}: تعذر الجلب (${it.localizedMessage ?: "لا يوجد اتصال"})") }
                entry = tafsirDao.getEntry(src.id, surahIndex, ayahNumber)
            }
            tafsirs[src.id] = entry?.text
        }

        val gharibText = tafsirs["gharib"]
        val gharibParsedWords = parseGharibIntoWords(gharibText)

        // Check Quran.com verified word-by-word API cache
        var wbwEntity = tafsirDao.getWordMeanings(surahIndex, ayahNumber)
        if (wbwEntity == null && fetchRemoteIfMissing) {
            runCatching { fetchAndCacheWordByWord(surahIndex, ayahNumber) }
            wbwEntity = tafsirDao.getWordMeanings(surahIndex, ayahNumber)
        }

        val apiWords = wbwEntity?.let { parseWordMeaningsJson(it.wordsJson, it.sourceLabel) }.orEmpty()
        val combinedWords = if (gharibParsedWords.isNotEmpty()) {
            // Merge Gharib authoritative Arabic word explanations first, plus word list from Quran.com if available
            val gharibMap = gharibParsedWords.associateBy { normalizeLoose(it.wordUthmani) }
            if (apiWords.isNotEmpty()) {
                apiWords.map { w ->
                    val g = gharibMap[normalizeLoose(w.wordUthmani)]
                    if (g != null) g else w
                }
            } else {
                gharibParsedWords
            }
        } else {
            apiWords
        }

        // Extract Asbab al-Nuzul ONLY when explicitly present in downloaded authentic classical tafsir
        val sabab = extractVerifiedSababNuzul(
            ibnKathir = tafsirs["ibn_kathir"],
            tabari = tafsirs["tabari"]
        )

        AyahStudyBundle(
            surahIndex = surahIndex,
            ayahNumber = ayahNumber,
            tafsirs = tafsirs,
            wordMeanings = combinedWords,
            wordMeaningsAvailable = combinedWords.isNotEmpty() || !gharibText.isNullOrBlank(),
            gharibSummary = gharibText,
            sababNuzul = sabab,
            errors = errors
        )
    }

    suspend fun downloadFullPack(
        sourceId: String,
        onProgress: (completedSurahs: Int, totalSurahs: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val src = SOURCES.firstOrNull { it.id == sourceId }
            ?: return@withContext Result.failure(IllegalArgumentException("مصدر غير معروف"))

        var downloaded = 0
        for (surah in 1..114) {
            val existing = tafsirDao.getEntry(src.id, surah, 1)
            if (existing == null) {
                val res = runCatching { fetchAndCacheSurahTafsir(src, surah) }
                if (res.isFailure) {
                    return@withContext Result.failure(
                        res.exceptionOrNull() ?: IllegalStateException("تعذر تنزيل سورة $surah")
                    )
                }
            }
            downloaded++
            onProgress(downloaded, 114)
        }
        Result.success(downloaded)
    }

    private fun fetchAndCacheSurahTafsir(src: TafsirSourceInfo, surahIndex: Int) {
        val url = "${src.remoteSourceUrl}/$surahIndex.json"
        val req = Request.Builder().url(url).header("User-Agent", "WirdQuranApp/1.0").build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty response")
            val jsonArray = JSONArray(body)
            val entries = ArrayList<TafsirEntryEntity>(jsonArray.length())
            val now = System.currentTimeMillis()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val ayahNum = obj.optInt("ayah", 0)
                val text = obj.optString("text", "").trim()
                if (ayahNum > 0 && text.isNotEmpty()) {
                    entries.add(
                        TafsirEntryEntity(
                            sourceId = src.id,
                            surahIndex = surahIndex,
                            ayahNumber = ayahNum,
                            text = text,
                            cachedAt = now
                        )
                    )
                }
            }
            if (entries.isNotEmpty()) {
                kotlinx.coroutines.runBlocking {
                    tafsirDao.insertEntries(entries)
                }
            }
        }
    }

    private fun fetchAndCacheWordByWord(surahIndex: Int, ayahNumber: Int) {
        val url = "https://api.quran.com/api/v4/verses/by_key/$surahIndex:$ayahNumber?words=true&word_fields=text_uthmani,text_imlaei&translation_fields=text"
        val req = Request.Builder().url(url).header("User-Agent", "WirdQuranApp/1.0").build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return
            val body = resp.body?.string() ?: return
            val root = JSONObject(body)
            val verse = root.optJSONObject("verse") ?: return
            val words = verse.optJSONArray("words") ?: return
            val cleaned = JSONArray()
            for (i in 0 until words.length()) {
                val w = words.getJSONObject(i)
                if (w.optString("char_type_name") != "word") continue
                val uthmani = w.optString("text_uthmani", "")
                val trans = w.optJSONObject("translation")?.optString("text", "") ?: ""
                if (uthmani.isNotBlank()) {
                    cleaned.put(
                        JSONObject().apply {
                            put("word", uthmani)
                            put("meaning", trans)
                        }
                    )
                }
            }
            if (cleaned.length() > 0) {
                kotlinx.coroutines.runBlocking {
                    tafsirDao.insertWordMeanings(
                        WordMeaningsCacheEntity(
                            surahIndex = surahIndex,
                            ayahNumber = ayahNumber,
                            wordsJson = cleaned.toString(),
                            sourceLabel = "Quran.com Corpus (كلمة بكلمة)"
                        )
                    )
                }
            }
        }
    }

    private fun parseGharibIntoWords(gharibText: String?): List<WordMeaningItem> {
        if (gharibText.isNullOrBlank()) return emptyList()
        val regex = Regex("﴿([^﴾]+)﴾\\s*:\\s*([^\\n]+)")
        return regex.findAll(gharibText).map { match ->
            WordMeaningItem(
                wordUthmani = match.groupValues[1].trim(),
                meaning = match.groupValues[2].trim(),
                sourceLabel = "الميسر في غريب القرآن (مجمع الملك فهد)"
            )
        }.toList()
    }

    private fun parseWordMeaningsJson(jsonStr: String, sourceLabel: String): List<WordMeaningItem> {
        return runCatching {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val word = obj.optString("word", "").trim()
                val meaning = obj.optString("meaning", "").trim()
                if (word.isEmpty()) null else WordMeaningItem(word, meaning, sourceLabel)
            }
        }.getOrDefault(emptyList())
    }

    private fun extractVerifiedSababNuzul(ibnKathir: String?, tabari: String?): String? {
        val candidates = listOf(
            "تفسير ابن كثير" to ibnKathir,
            "تفسير الطبري" to tabari
        )
        val markers = listOf("سبب النزول", "سبب نزول", "نزلت هذه الآية", "نزلت في")
        for ((sourceName, text) in candidates) {
            if (text.isNullOrBlank()) continue
            val paragraphs = text.split("\n\n", "\n")
            val match = paragraphs.firstOrNull { p -> markers.any { m -> p.contains(m) } }
            if (match != null) {
                return "$match\n\n(المصدر: $sourceName)"
            }
        }
        return null
    }

    private fun normalizeLoose(text: String): String =
        text.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "").trim()
}
