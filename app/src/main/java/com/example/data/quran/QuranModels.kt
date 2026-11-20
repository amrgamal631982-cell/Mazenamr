package com.example.data.quran

data class SurahMeta(
    val index: Int,
    val nameArabic: String,
    val transliteration: String,
    val nameEnglish: String,
    val ayahCount: Int,
    val revelationType: String, // "Meccan" or "Medinan"
    val revelationOrder: Int,
    val rukuCount: Int,
    val startPage: Int,
    val startJuz: Int
) {
    val revelationTypeArabic: String
        get() = if (revelationType == "Meccan") "مكية" else "مدنية"
}

data class Ayah(
    val id: Int, // 1..6236 global sequential index
    val surahIndex: Int,
    val ayahNumber: Int,
    val uthmaniText: String,
    val bismillahPrefix: String?,
    val juzNumber: Int,
    val hizbNumber: Int,      // 1..60
    val hizbQuarter: Int,     // 1..240
    val pageNumber: Int,      // 1..604
    val sajdaType: String?    // null, "recommended", "obligatory"
)

data class JuzMeta(
    val index: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startSurahName: String,
    val startPage: Int
)

data class HizbMeta(
    val index: Int, // 1..60
    val startSurah: Int,
    val startAyah: Int,
    val startSurahName: String,
    val juzNumber: Int,
    val startPage: Int
)

data class SajdaMeta(
    val index: Int,
    val surahIndex: Int,
    val ayahNumber: Int,
    val surahName: String,
    val type: String
)

data class QuranSearchResult(
    val surahIndex: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val uthmaniText: String
)

data class QuranAuditReport(
    val passed: Boolean,
    val surahCount: Int,
    val totalAyahCount: Int,
    val uthmaniXmlSha256: String,
    val dataXmlSha256: String,
    val mismatchedAyahs: Int,
    val duplicateOrMissingCount: Int,
    val details: String,
    val verifiedAtTimestamp: Long
)
