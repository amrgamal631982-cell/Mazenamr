package com.example.data.quran

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class ParsedQuranBundle(
    val surahs: List<SurahMeta>,
    val ayahs: List<Ayah>,
    val juzs: List<JuzMeta>,
    val hizbs: List<HizbMeta>,
    val sajdas: List<SajdaMeta>
)

private data class Boundary(
    val index: Int,
    val surah: Int,
    val aya: Int,
    val extra: String = ""
)

object QuranAssetParser {

    fun parse(
        uthmaniXmlStream: InputStream,
        dataXmlStream: InputStream
    ): ParsedQuranBundle {
        val meta = parseDataXml(dataXmlStream)
        val rawSurahNames = mutableMapOf<Int, String>()
        meta.surahs.forEach { rawSurahNames[it.index] = it.nameArabic }

        val juzStarts = meta.juzs.map { packKey(it.surah, it.aya) to it.index }
        val hizbQuarterStarts = meta.hizbQuarters.map { packKey(it.surah, it.aya) to it.index }
        val pageStarts = meta.pages.map { packKey(it.surah, it.aya) to it.index }
        val sajdaMap = meta.sajdas.associate { (it.surah to it.aya) to it.extra }

        val ayahs = mutableListOf<Ayah>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(uthmaniXmlStream, "UTF-8")
        }

        var currentSurah = 0
        var globalId = 0
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "sura" -> {
                        currentSurah = parser.getAttributeValue(null, "index").toInt()
                    }
                    "aya" -> {
                        val ayaNum = parser.getAttributeValue(null, "index").toInt()
                        val text = parser.getAttributeValue(null, "text")
                        val bismillah = parser.getAttributeValue(null, "bismillah")
                        globalId++
                        val key = packKey(currentSurah, ayaNum)
                        val juz = findActiveBoundary(juzStarts, key)
                        val quarter = findActiveBoundary(hizbQuarterStarts, key)
                        val hizb = ((quarter - 1) / 4) + 1
                        val page = findActiveBoundary(pageStarts, key)
                        val sajda = sajdaMap[currentSurah to ayaNum]
                        ayahs.add(
                            Ayah(
                                id = globalId,
                                surahIndex = currentSurah,
                                ayahNumber = ayaNum,
                                uthmaniText = text,
                                bismillahPrefix = bismillah,
                                juzNumber = juz,
                                hizbNumber = hizb,
                                hizbQuarter = quarter,
                                pageNumber = page,
                                sajdaType = sajda
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        val firstAyahBySurah = ayahs.groupBy { it.surahIndex }.mapValues { it.value.firstOrNull() }
        val surahs = meta.surahs.map { raw ->
            val first = firstAyahBySurah[raw.index]
            SurahMeta(
                index = raw.index,
                nameArabic = raw.nameArabic,
                transliteration = raw.transliteration,
                nameEnglish = raw.nameEnglish,
                ayahCount = raw.ayahCount,
                revelationType = raw.revelationType,
                revelationOrder = raw.revelationOrder,
                rukuCount = raw.rukuCount,
                startPage = first?.pageNumber ?: 1,
                startJuz = first?.juzNumber ?: 1
            )
        }

        val juzMetas = meta.juzs.map { b ->
            val p = findActiveBoundary(pageStarts, packKey(b.surah, b.aya))
            JuzMeta(
                index = b.index,
                startSurah = b.surah,
                startAyah = b.aya,
                startSurahName = rawSurahNames[b.surah] ?: "",
                startPage = p
            )
        }

        val hizbMetas = meta.hizbQuarters
            .filter { (it.index - 1) % 4 == 0 }
            .map { b ->
                val hizbNum = ((b.index - 1) / 4) + 1
                val key = packKey(b.surah, b.aya)
                HizbMeta(
                    index = hizbNum,
                    startSurah = b.surah,
                    startAyah = b.aya,
                    startSurahName = rawSurahNames[b.surah] ?: "",
                    juzNumber = findActiveBoundary(juzStarts, key),
                    startPage = findActiveBoundary(pageStarts, key)
                )
            }

        val sajdaMetas = meta.sajdas.map { b ->
            SajdaMeta(
                index = b.index,
                surahIndex = b.surah,
                ayahNumber = b.aya,
                surahName = rawSurahNames[b.surah] ?: "",
                type = b.extra
            )
        }

        return ParsedQuranBundle(
            surahs = surahs,
            ayahs = ayahs,
            juzs = juzMetas,
            hizbs = hizbMetas,
            sajdas = sajdaMetas
        )
    }

    private data class RawSurah(
        val index: Int,
        val nameArabic: String,
        val transliteration: String,
        val nameEnglish: String,
        val ayahCount: Int,
        val revelationType: String,
        val revelationOrder: Int,
        val rukuCount: Int
    )

    private data class ParsedDataXml(
        val surahs: List<RawSurah>,
        val juzs: List<Boundary>,
        val hizbQuarters: List<Boundary>,
        val pages: List<Boundary>,
        val sajdas: List<Boundary>
    )

    private fun parseDataXml(stream: InputStream): ParsedDataXml {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, "UTF-8")
        }
        val surahs = mutableListOf<RawSurah>()
        val juzs = mutableListOf<Boundary>()
        val hizbs = mutableListOf<Boundary>()
        val pages = mutableListOf<Boundary>()
        val sajdas = mutableListOf<Boundary>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "sura" -> surahs.add(
                        RawSurah(
                            index = parser.getAttributeValue(null, "index").toInt(),
                            nameArabic = parser.getAttributeValue(null, "name"),
                            transliteration = parser.getAttributeValue(null, "tname"),
                            nameEnglish = parser.getAttributeValue(null, "ename"),
                            ayahCount = parser.getAttributeValue(null, "ayas").toInt(),
                            revelationType = parser.getAttributeValue(null, "type"),
                            revelationOrder = parser.getAttributeValue(null, "order").toInt(),
                            rukuCount = parser.getAttributeValue(null, "rukus").toInt()
                        )
                    )
                    "juz" -> juzs.add(
                        Boundary(
                            index = parser.getAttributeValue(null, "index").toInt(),
                            surah = parser.getAttributeValue(null, "sura").toInt(),
                            aya = parser.getAttributeValue(null, "aya").toInt()
                        )
                    )
                    "quarter" -> hizbs.add(
                        Boundary(
                            index = parser.getAttributeValue(null, "index").toInt(),
                            surah = parser.getAttributeValue(null, "sura").toInt(),
                            aya = parser.getAttributeValue(null, "aya").toInt()
                        )
                    )
                    "page" -> pages.add(
                        Boundary(
                            index = parser.getAttributeValue(null, "index").toInt(),
                            surah = parser.getAttributeValue(null, "sura").toInt(),
                            aya = parser.getAttributeValue(null, "aya").toInt()
                        )
                    )
                    "sajda" -> sajdas.add(
                        Boundary(
                            index = parser.getAttributeValue(null, "index").toInt(),
                            surah = parser.getAttributeValue(null, "sura").toInt(),
                            aya = parser.getAttributeValue(null, "aya").toInt(),
                            extra = parser.getAttributeValue(null, "type") ?: "recommended"
                        )
                    )
                }
            }
            eventType = parser.next()
        }
        return ParsedDataXml(surahs, juzs, hizbs, pages, sajdas)
    }

    private fun packKey(surah: Int, aya: Int): Int = surah * 1000 + aya

    private fun findActiveBoundary(sortedStarts: List<Pair<Int, Int>>, targetKey: Int): Int {
        var low = 0
        var high = sortedStarts.lastIndex
        var result = 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val (key, index) = sortedStarts[mid]
            if (key <= targetKey) {
                result = index
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
