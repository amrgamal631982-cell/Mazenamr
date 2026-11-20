package com.example.data.quran

import java.io.InputStream
import java.security.MessageDigest

object QuranIntegrityVerifier {
    const val TANZIL_VERSION = "Tanzil Quran Text (Uthmani, Version 1.1)"
    const val TANZIL_SOURCE_URL = "https://tanzil.net"
    const val TANZIL_LICENSE = "Creative Commons Attribution 3.0"
    const val TANZIL_COPYRIGHT_NOTICE =
        "Tanzil Quran Text (Uthmani, Version 1.1) — Copyright (C) 2007-2026 Tanzil Project — License: CC BY 3.0 (tanzil.net). يحفظ هذا النص كما ورد دون أي تغيير في الحروف أو التشكيل."

    const val EXPECTED_UTHMANI_XML_SHA256 =
        "33f4113df84c1bab0ee2eb69616139fda619bdd56fc163a72532daaf0431bf57"
    const val EXPECTED_DATA_XML_SHA256 =
        "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a"

    const val EXPECTED_SURAH_COUNT = 114
    const val EXPECTED_TOTAL_AYAH_COUNT = 6236
    const val EXPECTED_JUZ_COUNT = 30
    const val EXPECTED_HIZB_COUNT = 60
    const val EXPECTED_PAGE_COUNT = 604
    const val EXPECTED_SAJDA_COUNT = 15

    // Standard Hafs 'an Asim ayah counts for all 114 surahs (sum = 6236)
    val EXPECTED_AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        inputStream.use { stream ->
            var read = stream.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Normalizes Arabic text STRICTLY for offline search indexing & query matching.
     * NEVER used to alter or display the Uthmani Quran text.
     */
    fun normalizeArabicForSearch(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                // Strip tashkeel, Quranic annotation marks, small high letters, pause signs, tatweel
                ch in '\u064B'..'\u065F' -> Unit
                ch == '\u0670' -> Unit // Superscript alef
                ch in '\u06D6'..'\u06ED' -> Unit // Quranic pause/sajda/rub marks
                ch == '\u0640' -> Unit // Tatweel
                ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ' -> sb.append('ا')
                ch == 'ى' -> sb.append('ي')
                ch == 'ة' -> sb.append('ه')
                ch == 'ؤ' -> sb.append('و')
                ch == 'ئ' -> sb.append('ي')
                ch == 'ۥ' || ch == 'ۦ' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    fun verifyParsedData(
        surahs: List<SurahMeta>,
        ayahs: List<Ayah>,
        uthmaniSha256: String,
        dataSha256: String,
        dbComparisonTexts: Map<Pair<Int, Int>, String>? = null
    ): QuranAuditReport {
        val errors = mutableListOf<String>()
        var mismatched = 0
        var duplicateOrMissing = 0

        if (uthmaniSha256 != EXPECTED_UTHMANI_XML_SHA256) {
            errors.add("تطابق بصمة SHA-256 لملف النص العثماني فشل")
        }
        if (dataSha256 != EXPECTED_DATA_XML_SHA256) {
            errors.add("تطابق بصمة SHA-256 لملف البيانات الهيكلية فشل")
        }
        if (surahs.size != EXPECTED_SURAH_COUNT) {
            errors.add("عدد السور غير مطابق: ${surahs.size} بدل $EXPECTED_SURAH_COUNT")
        }
        if (ayahs.size != EXPECTED_TOTAL_AYAH_COUNT) {
            errors.add("مجموع الآيات غير مطابق: ${ayahs.size} بدل $EXPECTED_TOTAL_AYAH_COUNT")
        }

        val ayahsBySurah = ayahs.groupBy { it.surahIndex }
        for (s in 1..EXPECTED_SURAH_COUNT) {
            val expectedCount = EXPECTED_AYAH_COUNTS[s - 1]
            val list = ayahsBySurah[s].orEmpty()
            if (list.size != expectedCount) {
                duplicateOrMissing += kotlin.math.abs(expectedCount - list.size)
                errors.add("سورة $s: عدد الآيات ${list.size} بدل $expectedCount")
            }
            val seen = HashSet<Int>(list.size)
            for ((idx, a) in list.withIndex()) {
                if (a.ayahNumber != idx + 1 || !seen.add(a.ayahNumber)) {
                    duplicateOrMissing++
                    errors.add("ترتيب أو تكرار غير صحيح في سورة $s آية ${a.ayahNumber}")
                }
                if (a.uthmaniText.isBlank()) {
                    mismatched++
                    errors.add("نص فارغ في سورة $s آية ${a.ayahNumber}")
                }
                if (dbComparisonTexts != null) {
                    val dbText = dbComparisonTexts[a.surahIndex to a.ayahNumber]
                    if (dbText != a.uthmaniText) {
                        mismatched++
                        errors.add("اختلاف نصي في قاعدة البيانات عند سورة $s آية ${a.ayahNumber}")
                    }
                }
            }
        }

        val passed = errors.isEmpty()
        val summary = if (passed) {
            "تم التحقق بنجاح: 114 سورة و 6,236 آية مطابقة حرفياً وتشكيلًا لنص مشروع تنزيل (SHA-256 موثق)."
        } else {
            errors.take(5).joinToString(" | ")
        }

        return QuranAuditReport(
            passed = passed,
            surahCount = surahs.size,
            totalAyahCount = ayahs.size,
            uthmaniXmlSha256 = uthmaniSha256,
            dataXmlSha256 = dataSha256,
            mismatchedAyahs = mismatched,
            duplicateOrMissingCount = duplicateOrMissing,
            details = summary,
            verifiedAtTimestamp = System.currentTimeMillis()
        )
    }
}
