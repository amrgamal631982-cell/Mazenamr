package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.data.duas.DuaCategory
import com.example.data.duas.DuasRepository
import com.example.data.local.WirdDatabase
import com.example.data.quran.QuranAssetParser
import com.example.data.quran.QuranIntegrityVerifier
import com.example.data.quran.QuranRepository
import com.example.data.reciters.ReciterCatalog
import com.example.qibla.QiblaCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranIntegrityRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: WirdDatabase
    private lateinit var quranRepository: QuranRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, WirdDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        quranRepository = QuranRepository(
            context = context,
            quranDao = database.quranDao(),
            bookmarkDao = database.bookmarkDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify Tanzil XML SHA256, 114 surahs, 6236 ayahs, and verbatim Room equality`() = runBlocking {
        // 1. Verify raw XML SHA-256 hashes match imported Tanzil files
        val uthmaniSha = context.assets.open("quran/quran-uthmani.xml").use {
            QuranIntegrityVerifier.computeSha256(it)
        }
        val dataSha = context.assets.open("quran/quran-data.xml").use {
            QuranIntegrityVerifier.computeSha256(it)
        }
        assertEquals(QuranIntegrityVerifier.EXPECTED_UTHMANI_XML_SHA256, uthmaniSha)
        assertEquals(QuranIntegrityVerifier.EXPECTED_DATA_XML_SHA256, dataSha)

        // 2. Parse raw XML independently
        val parsed = context.assets.open("quran/quran-uthmani.xml").use { u ->
            context.assets.open("quran/quran-data.xml").use { d ->
                QuranAssetParser.parse(u, d)
            }
        }
        assertEquals(114, parsed.surahs.size)
        assertEquals(6236, parsed.ayahs.size)
        assertEquals(30, parsed.juzs.size)
        assertEquals(60, parsed.hizbs.size)
        assertEquals(15, parsed.sajdas.size)

        // 3. Seed Room database and run full audit comparing EVERY SINGLE AYAH against XML
        quranRepository.ensureInitialized()
        val audit = quranRepository.runFullAudit()
        assertTrue("Audit failed: ${audit.details}", audit.passed)
        assertEquals(114, audit.surahCount)
        assertEquals(6236, audit.totalAyahCount)
        assertEquals(0, audit.mismatchedAyahs)
        assertEquals(0, audit.duplicateOrMissingCount)

        // 4. Verify Surah 9 (At-Tawbah) has NO Bismillah prefix while Surah 2 has Bismillah
        val tawbah1 = quranRepository.getAyah(9, 1)
        val baqarah1 = quranRepository.getAyah(2, 1)
        assertNotNull(tawbah1)
        assertNotNull(baqarah1)
        assertNull("Surah 9 must not have Bismillah", tawbah1?.bismillahPrefix)
        assertNotNull("Surah 2 must have Bismillah", baqarah1?.bismillahPrefix)

        // 5. Verify exact ayah counts for every surah 1..114
        for (s in 1..114) {
            val surahAyahs = quranRepository.getSurahAyahs(s)
            assertEquals(
                "Ayah count mismatch for surah $s",
                QuranIntegrityVerifier.EXPECTED_AYAH_COUNTS[s - 1],
                surahAyahs.size
            )
            surahAyahs.forEachIndexed { idx, ayah ->
                assertEquals(idx + 1, ayah.ayahNumber)
            }
        }
    }

    @Test
    fun `verify offline search handles hamza and tashkeel without altering Uthmani text`() = runBlocking {
        quranRepository.ensureInitialized()
        val byKey = quranRepository.searchQuran("2:255")
        assertEquals(1, byKey.size)
        assertEquals(2, byKey.first().surahIndex)
        assertEquals(255, byKey.first().ayahNumber)

        val byNormalized = quranRepository.searchQuran("الله لا اله الا هو الحي القيوم")
        assertTrue(byNormalized.any { it.surahIndex == 2 && it.ayahNumber == 255 })
        // Stored Uthmani text must retain full tashkeel
        val original = quranRepository.getAyah(2, 255)!!
        assertEquals(original.uthmaniText, byNormalized.first { it.surahIndex == 2 && it.ayahNumber == 255 }.uthmaniText)
    }

    @Test
    fun `verify offline Adhkar and Duas sources load all required categories`() = runBlocking {
        quranRepository.ensureInitialized()
        val adhkarRepo = AdhkarRepository(context)
        val adhkarMap = adhkarRepo.getAllByCategory()
        AdhkarCategory.entries.forEach { cat ->
            val items = adhkarMap[cat].orEmpty()
            assertTrue("Adhkar category ${cat.titleArabic} must not be empty", items.isNotEmpty())
            items.forEach { item ->
                assertTrue(item.arabicText.isNotBlank())
                assertTrue(item.reference.isNotBlank())
            }
        }

        val duasRepo = DuasRepository(context, quranRepository)
        val duasMap = duasRepo.getAllByCategory()
        DuaCategory.entries.forEach { cat ->
            val items = duasMap[cat].orEmpty()
            assertTrue("Dua category ${cat.titleArabic} must not be empty", items.isNotEmpty())
            items.forEach { dua ->
                assertTrue(dua.arabicText.isNotBlank())
                assertTrue(dua.sourceCitation.isNotBlank())
            }
        }
    }

    @Test
    fun `verify 5 reciters catalog and Qibla trigonometry accuracy`() {
        assertEquals(5, ReciterCatalog.RECITERS.size)
        val names = ReciterCatalog.RECITERS.map { it.nameArabic }
        assertTrue(names.contains("محمود خليل الحصري"))
        assertTrue(names.contains("عبد الباسط عبد الصمد"))
        assertTrue(names.contains("محمد صديق المنشاوي"))
        assertTrue(names.contains("ياسر الدوسري"))
        assertTrue(names.contains("ماهر المعيقلي"))

        // Cairo Qibla bearing is ~136 degrees (Southeast)
        val cairoQibla = QiblaCalculator.calculateQiblaBearing(30.0444, 31.2357)
        assertTrue("Cairo Qibla expected ~136°, got $cairoQibla", cairoQibla in 130.0..142.0)

        // Riyadh Qibla bearing is ~244 degrees (West-Southwest)
        val riyadhQibla = QiblaCalculator.calculateQiblaBearing(24.7136, 46.6753)
        assertTrue("Riyadh Qibla expected ~244°, got $riyadhQibla", riyadhQibla in 238.0..250.0)
    }
}
