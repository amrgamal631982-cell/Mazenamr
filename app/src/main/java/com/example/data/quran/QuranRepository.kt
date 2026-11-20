package com.example.data.quran

import android.content.Context
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkFolderEntity
import com.example.data.local.QuranDao
import com.example.data.local.SurahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class QuranRepository(
    private val context: Context,
    private val quranDao: QuranDao,
    private val bookmarkDao: BookmarkDao
) {
    private val initMutex = Mutex()

    @Volatile
    private var cachedBundle: ParsedQuranBundle? = null

    @Volatile
    private var lastAuditReport: QuranAuditReport? = null

    suspend fun ensureInitialized(): ParsedQuranBundle = withContext(Dispatchers.IO) {
        cachedBundle?.let { return@withContext it }
        initMutex.withLock {
            cachedBundle?.let { return@withLock it }

            val bundle = context.assets.open("quran/quran-uthmani.xml").use { uthmaniStream ->
                context.assets.open("quran/quran-data.xml").use { dataStream ->
                    QuranAssetParser.parse(uthmaniStream, dataStream)
                }
            }

            if (quranDao.getSurahCount() != QuranIntegrityVerifier.EXPECTED_SURAH_COUNT ||
                quranDao.getAyahCount() != QuranIntegrityVerifier.EXPECTED_TOTAL_AYAH_COUNT
            ) {
                val surahEntities = bundle.surahs.map {
                    SurahEntity(
                        index = it.index,
                        nameArabic = it.nameArabic,
                        transliteration = it.transliteration,
                        nameEnglish = it.nameEnglish,
                        ayahCount = it.ayahCount,
                        revelationType = it.revelationType,
                        revelationOrder = it.revelationOrder,
                        rukuCount = it.rukuCount,
                        startPage = it.startPage,
                        startJuz = it.startJuz
                    )
                }
                val ayahEntities = bundle.ayahs.map {
                    AyahEntity(
                        id = it.id,
                        surahIndex = it.surahIndex,
                        ayahNumber = it.ayahNumber,
                        uthmaniText = it.uthmaniText,
                        normalizedSearchText = QuranIntegrityVerifier.normalizeArabicForSearch(it.uthmaniText),
                        bismillahPrefix = it.bismillahPrefix,
                        juzNumber = it.juzNumber,
                        hizbNumber = it.hizbNumber,
                        hizbQuarter = it.hizbQuarter,
                        pageNumber = it.pageNumber,
                        sajdaType = it.sajdaType
                    )
                }
                quranDao.seedQuranOnce(surahEntities, ayahEntities)
            }

            // Ensure default bookmark folders exist
            listOf("المفضلة", "مواضع القراءة", "آيات الحفظ", "التدبر").forEachIndexed { idx, name ->
                bookmarkDao.insertFolder(BookmarkFolderEntity(id = (idx + 1).toLong(), name = name))
            }

            cachedBundle = bundle
            bundle
        }
    }

    suspend fun getAllSurahs(): List<SurahMeta> = ensureInitialized().surahs

    suspend fun getAllJuzs(): List<JuzMeta> = ensureInitialized().juzs

    suspend fun getAllHizbs(): List<HizbMeta> = ensureInitialized().hizbs

    suspend fun getAllSajdas(): List<SajdaMeta> = ensureInitialized().sajdas

    suspend fun getSurah(surahIndex: Int): SurahMeta? =
        ensureInitialized().surahs.firstOrNull { it.index == surahIndex }

    suspend fun getSurahAyahs(surahIndex: Int): List<Ayah> = withContext(Dispatchers.IO) {
        ensureInitialized()
        quranDao.getAyahsBySurah(surahIndex).map { it.toDomain() }
    }

    suspend fun getPageAyahs(pageNumber: Int): List<Ayah> = withContext(Dispatchers.IO) {
        ensureInitialized()
        quranDao.getAyahsByPage(pageNumber.coerceIn(1, 604)).map { it.toDomain() }
    }

    suspend fun getJuzAyahs(juzNumber: Int): List<Ayah> = withContext(Dispatchers.IO) {
        ensureInitialized()
        quranDao.getAyahsByJuz(juzNumber.coerceIn(1, 30)).map { it.toDomain() }
    }

    suspend fun getHizbAyahs(hizbNumber: Int): List<Ayah> = withContext(Dispatchers.IO) {
        ensureInitialized()
        quranDao.getAyahsByHizb(hizbNumber.coerceIn(1, 60)).map { it.toDomain() }
    }

    suspend fun getAyah(surahIndex: Int, ayahNumber: Int): Ayah? = withContext(Dispatchers.IO) {
        ensureInitialized()
        quranDao.getAyah(surahIndex, ayahNumber)?.toDomain()
    }

    suspend fun searchQuran(rawQuery: String): List<QuranSearchResult> = withContext(Dispatchers.IO) {
        val bundle = ensureInitialized()
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val surahByIndex = bundle.surahs.associateBy { it.index }

        // Support "surah:ayah" or "2:255" pattern
        val refMatch = Regex("^(\\d{1,3})\\s*[:\\-]\\s*(\\d{1,3})$").find(trimmed)
        if (refMatch != null) {
            val s = refMatch.groupValues[1].toIntOrNull()
            val a = refMatch.groupValues[2].toIntOrNull()
            if (s != null && a != null) {
                val ayah = quranDao.getAyah(s, a)?.toDomain()
                if (ayah != null) {
                    return@withContext listOf(
                        QuranSearchResult(
                            surahIndex = ayah.surahIndex,
                            surahName = surahByIndex[ayah.surahIndex]?.nameArabic ?: "",
                            ayahNumber = ayah.ayahNumber,
                            pageNumber = ayah.pageNumber,
                            juzNumber = ayah.juzNumber,
                            uthmaniText = ayah.uthmaniText
                        )
                    )
                }
            }
        }

        val normalizedQuery = QuranIntegrityVerifier.normalizeArabicForSearch(trimmed)
        if (normalizedQuery.isBlank()) return@withContext emptyList()

        // Also check if user entered "سورة البقرة 255" or surah name + number
        val tokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        val numberToken = tokens.lastOrNull()?.toIntOrNull()
        if (numberToken != null && tokens.size >= 2) {
            val nameQuery = tokens.dropLast(1).joinToString(" ").removePrefix("سوره ").trim()
            val matchedSurah = bundle.surahs.firstOrNull {
                QuranIntegrityVerifier.normalizeArabicForSearch(it.nameArabic).contains(nameQuery)
            }
            if (matchedSurah != null) {
                val exactAyah = quranDao.getAyah(matchedSurah.index, numberToken)?.toDomain()
                if (exactAyah != null) {
                    return@withContext listOf(
                        QuranSearchResult(
                            surahIndex = exactAyah.surahIndex,
                            surahName = matchedSurah.nameArabic,
                            ayahNumber = exactAyah.ayahNumber,
                            pageNumber = exactAyah.pageNumber,
                            juzNumber = exactAyah.juzNumber,
                            uthmaniText = exactAyah.uthmaniText
                        )
                    )
                }
            }
        }

        val results = mutableListOf<QuranSearchResult>()

        // Match Surah names first (return first ayah of matching surahs if query matches a surah name)
        val cleanSurahQuery = normalizedQuery.removePrefix("سوره ").trim()
        if (cleanSurahQuery.length >= 2) {
            bundle.surahs.filter {
                QuranIntegrityVerifier.normalizeArabicForSearch(it.nameArabic).contains(cleanSurahQuery)
            }.take(5).forEach { s ->
                quranDao.getAyah(s.index, 1)?.toDomain()?.let { first ->
                    results.add(
                        QuranSearchResult(
                            surahIndex = s.index,
                            surahName = s.nameArabic,
                            ayahNumber = 1,
                            pageNumber = first.pageNumber,
                            juzNumber = first.juzNumber,
                            uthmaniText = first.uthmaniText
                        )
                    )
                }
            }
        }

        val matchedAyahs = quranDao.searchAyahsNormalized(normalizedQuery, limit = 120)
        matchedAyahs.forEach { entity ->
            if (results.none { it.surahIndex == entity.surahIndex && it.ayahNumber == entity.ayahNumber }) {
                results.add(
                    QuranSearchResult(
                        surahIndex = entity.surahIndex,
                        surahName = surahByIndex[entity.surahIndex]?.nameArabic ?: "",
                        ayahNumber = entity.ayahNumber,
                        pageNumber = entity.pageNumber,
                        juzNumber = entity.juzNumber,
                        uthmaniText = entity.uthmaniText
                    )
                )
            }
        }

        results
    }

    suspend fun runFullAudit(): QuranAuditReport = withContext(Dispatchers.IO) {
        val bundle = ensureInitialized()
        val uthmaniSha = context.assets.open("quran/quran-uthmani.xml").use {
            QuranIntegrityVerifier.computeSha256(it)
        }
        val dataSha = context.assets.open("quran/quran-data.xml").use {
            QuranIntegrityVerifier.computeSha256(it)
        }
        val dbTexts = quranDao.getAllAyahsOrdered().associate {
            (it.surahIndex to it.ayahNumber) to it.uthmaniText
        }
        val report = QuranIntegrityVerifier.verifyParsedData(
            surahs = bundle.surahs,
            ayahs = bundle.ayahs,
            uthmaniSha256 = uthmaniSha,
            dataSha256 = dataSha,
            dbComparisonTexts = dbTexts
        )
        lastAuditReport = report
        report
    }

    fun getLastAuditReport(): QuranAuditReport? = lastAuditReport

    private fun AyahEntity.toDomain(): Ayah = Ayah(
        id = id,
        surahIndex = surahIndex,
        ayahNumber = ayahNumber,
        uthmaniText = uthmaniText,
        bismillahPrefix = bismillahPrefix,
        juzNumber = juzNumber,
        hizbNumber = hizbNumber,
        hizbQuarter = hizbQuarter,
        pageNumber = pageNumber,
        sajdaType = sajdaType
    )
}
