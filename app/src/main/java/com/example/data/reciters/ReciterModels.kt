package com.example.data.reciters

enum class ReciterRegion(val labelArabic: String) {
    EGYPT("القراء المصريون"),
    OTHER("قراء الحرمين والعالم الإسلامي")
}

data class ReciterInfo(
    val id: String,
    val nameArabic: String,
    val region: ReciterRegion,
    val styleArabic: String,
    val riwayahArabic: String,
    val bioShort: String,
    val everyAyahBaseUrl: String,
    val quranComChapterReciterId: Int?, // For full surah + timing segments when available
    val mp3QuranBaseUrl: String?,       // For full surah MP3 when quran.com chapter ID isn't used
    val sourceLabel: String
) {
    fun ayahAudioUrl(surahIndex: Int, ayahNumber: Int): String {
        val sss = surahIndex.toString().padStart(3, '0')
        val aaa = ayahNumber.toString().padStart(3, '0')
        return "$everyAyahBaseUrl$sss$aaa.mp3"
    }

    fun mp3QuranSurahUrl(surahIndex: Int): String? {
        val base = mp3QuranBaseUrl ?: return null
        val sss = surahIndex.toString().padStart(3, '0')
        return "$base$sss.mp3"
    }
}

data class AyahTimingSegment(
    val surahIndex: Int,
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long
)

data class SurahAudioResolution(
    val reciter: ReciterInfo,
    val surahIndex: Int,
    val fullSurahUrl: String?,
    val localFullSurahPath: String?,
    val timings: List<AyahTimingSegment>,
    val ayahUrlsAvailable: Boolean,
    val verifiedAt: Long
)

object ReciterCatalog {
    val RECITERS = listOf(
        ReciterInfo(
            id = "husary",
            nameArabic = "محمود خليل الحصري",
            region = ReciterRegion.EGYPT,
            styleArabic = "مرتل",
            riwayahArabic = "حفص عن عاصم",
            bioShort = "شيخ عموم المقارئ المصرية، من أئمة الإتقان والتجويد في العصر الحديث.",
            everyAyahBaseUrl = "https://everyayah.com/data/Husary_128kbps/",
            quranComChapterReciterId = 6,
            mp3QuranBaseUrl = "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/",
            sourceLabel = "EveryAyah (Husary_128kbps) و Quran.com (Reciter #6)"
        ),
        ReciterInfo(
            id = "abdul_basit",
            nameArabic = "عبد الباسط عبد الصمد",
            region = ReciterRegion.EGYPT,
            styleArabic = "مرتل",
            riwayahArabic = "حفص عن عاصم",
            bioShort = "أحد أشهر قراء العالم الإسلامي من مصر، تميز بعذوبة الصوت وقوة الأداء.",
            everyAyahBaseUrl = "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/",
            quranComChapterReciterId = 2,
            mp3QuranBaseUrl = null,
            sourceLabel = "EveryAyah (Abdul_Basit_Murattal_192kbps) و Quran.com (Reciter #2)"
        ),
        ReciterInfo(
            id = "minshawi",
            nameArabic = "محمد صديق المنشاوي",
            region = ReciterRegion.EGYPT,
            styleArabic = "مرتل",
            riwayahArabic = "حفص عن عاصم",
            bioShort = "قارئ مصري بارز عُرف بالخشوع والإتقان في الترتيل والتجويد.",
            everyAyahBaseUrl = "https://everyayah.com/data/Minshawy_Murattal_128kbps/",
            quranComChapterReciterId = 9,
            mp3QuranBaseUrl = "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/",
            sourceLabel = "EveryAyah (Minshawy_Murattal_128kbps) و Quran.com (Reciter #9)"
        ),
        ReciterInfo(
            id = "yasser_dosari",
            nameArabic = "ياسر الدوسري",
            region = ReciterRegion.OTHER,
            styleArabic = "مرتل",
            riwayahArabic = "حفص عن عاصم",
            bioShort = "إمام وخطيب المسجد الحرام، يتميز بأداء خاشع متقن.",
            everyAyahBaseUrl = "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/",
            quranComChapterReciterId = null,
            mp3QuranBaseUrl = "https://cdn.mp3quran.net/audio/yasser-dosari/r1/",
            sourceLabel = "EveryAyah (Yasser_Ad-Dussary_128kbps) و MP3Quran (yasser-dosari/r1)"
        ),
        ReciterInfo(
            id = "maher_muaiqly",
            nameArabic = "ماهر المعيقلي",
            region = ReciterRegion.OTHER,
            styleArabic = "مرتل",
            riwayahArabic = "حفص عن عاصم",
            bioShort = "إمام وخطيب المسجد الحرام، عُرف بالسكينة والوضوح في التلاوة.",
            everyAyahBaseUrl = "https://everyayah.com/data/MaherAlMuaiqly128kbps/",
            quranComChapterReciterId = null,
            mp3QuranBaseUrl = "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/",
            sourceLabel = "EveryAyah (MaherAlMuaiqly128kbps) و MP3Quran (maher-muaiqly/r1)"
        )
    )

    fun findById(id: String): ReciterInfo = RECITERS.firstOrNull { it.id == id } ?: RECITERS.first()
}
