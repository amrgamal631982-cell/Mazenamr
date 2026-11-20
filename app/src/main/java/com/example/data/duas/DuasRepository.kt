package com.example.data.duas

import android.content.Context
import com.example.data.quran.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class DuaCategory(val titleArabic: String) {
    QURANIC("أدعية القرآن الكريم"),
    PROPHETS("أدعية الأنبياء"),
    SUNNAH("أدعية مأثورة من السنة"),
    ISTIGHFAR("أدعية الاستغفار"),
    TAWBAH("أدعية التوبة"),
    DISTRESS("أدعية تفريج الكرب"),
    KNOWLEDGE_RIZQ("أدعية العلم والرزق"),
    PARENTS("أدعية الوالدين"),
    GUIDANCE("أدعية الهداية والثبات")
}

enum class DuaOriginBadge(val label: String) {
    QURAN("قرآن كريم"),
    PROPHETIC_SUNNAH("سنة نبوية موثقة")
}

data class DuaItem(
    val id: String,
    val category: DuaCategory,
    val originBadge: DuaOriginBadge,
    val title: String,
    val arabicText: String,
    val sourceCitation: String
)

private data class QuranDuaRef(
    val category: DuaCategory,
    val title: String,
    val surah: Int,
    val startAyah: Int,
    val endAyah: Int = startAyah
)

private data class HisnDuaRef(
    val category: DuaCategory,
    val chapterKey: String
)

class DuasRepository(
    private val context: Context,
    private val quranRepository: QuranRepository
) {
    private val quranDuaRefs = listOf(
        // أدعية القرآن الكريم
        QuranDuaRef(DuaCategory.QURANIC, "ربنا آتنا في الدنيا حسنة", 2, 201),
        QuranDuaRef(DuaCategory.QURANIC, "ربنا لا تؤاخذنا إن نسينا أو أخطأنا", 2, 286),
        QuranDuaRef(DuaCategory.QURANIC, "ربنا تقبل منا إنك أنت السميع العليم", 2, 127),
        QuranDuaRef(DuaCategory.QURANIC, "ربنا هب لنا من أزواجنا وذرياتنا قرة أعين", 25, 74),
        QuranDuaRef(DuaCategory.QURANIC, "ربنا اغفر لنا ولإخواننا الذين سبقونا بالإيمان", 59, 10),

        // أدعية الأنبياء
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء آدم وحواء عليهما السلام", 7, 23),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء نوح عليه السلام", 11, 47),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء إبراهيم عليه السلام للذرية والصلاة", 14, 40, 41),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء يوسف عليه السلام", 12, 101),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء أيوب عليه السلام", 21, 83),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء يونس عليه السلام في الظلمات", 21, 87),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء موسى عليه السلام لشرح الصدر", 20, 25, 28),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء موسى عليه السلام للرزق والخير", 28, 24),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء سليمان عليه السلام للشكر", 27, 19),
        QuranDuaRef(DuaCategory.PROPHETS, "دعاء زكريا عليه السلام", 21, 89),

        // أدعية الاستغفار
        QuranDuaRef(DuaCategory.ISTIGHFAR, "دعاء المتقين بالاستغفار", 3, 16),
        QuranDuaRef(DuaCategory.ISTIGHFAR, "ربنا فاغفر لنا ذنوبنا وكفر عنا سيئاتنا", 3, 193),
        QuranDuaRef(DuaCategory.ISTIGHFAR, "دعاء استغفار نوح عليه السلام", 71, 28),

        // أدعية التوبة
        QuranDuaRef(DuaCategory.TAWBAH, "ربنا ظلمنا أنفسنا", 7, 23),
        QuranDuaRef(DuaCategory.TAWBAH, "دعاء موسى عليه السلام في التوبة", 28, 16),
        QuranDuaRef(DuaCategory.TAWBAH, "دعاء النور والمغفرة", 66, 8),

        // أدعية تفريج الكرب
        QuranDuaRef(DuaCategory.DISTRESS, "دعاء الكرب في القرآن (لا إله إلا أنت سبحانك)", 21, 87),
        QuranDuaRef(DuaCategory.DISTRESS, "دعاء الضر والرحمة", 21, 83),
        QuranDuaRef(DuaCategory.DISTRESS, "دعاء التوكل والكفاية", 9, 129),

        // أدعية العلم والرزق
        QuranDuaRef(DuaCategory.KNOWLEDGE_RIZQ, "دعاء زيادة العلم", 20, 114),
        QuranDuaRef(DuaCategory.KNOWLEDGE_RIZQ, "دعاء موسى عليه السلام للخير والرزق", 28, 24),
        QuranDuaRef(DuaCategory.KNOWLEDGE_RIZQ, "دعاء عيسى عليه السلام للرزق", 5, 114),

        // أدعية الوالدين
        QuranDuaRef(DuaCategory.PARENTS, "رب اغفر لي ولوالدي وللمؤمنين", 14, 41),
        QuranDuaRef(DuaCategory.PARENTS, "رب ارحمهما كما ربياني صغيراً", 17, 24),
        QuranDuaRef(DuaCategory.PARENTS, "دعاء البر بالوالدين والذرية", 46, 15),
        QuranDuaRef(DuaCategory.PARENTS, "رب اغفر لي ولوالدي ولمن دخل بيتي مؤمناً", 71, 28),

        // أدعية الهداية والثبات
        QuranDuaRef(DuaCategory.GUIDANCE, "اهدنا الصراط المستقيم", 1, 6, 7),
        QuranDuaRef(DuaCategory.GUIDANCE, "ربنا لا تزغ قلوبنا بعد إذ هديتنا", 3, 8),
        QuranDuaRef(DuaCategory.GUIDANCE, "ربنا أفرغ علينا صبراً وثبت أقدامنا", 2, 250),
        QuranDuaRef(DuaCategory.GUIDANCE, "ربنا آتنا من لدنك رحمة وهيئ لنا من أمرنا رشداً", 18, 10)
    )

    private val hisnDuaRefs = listOf(
        HisnDuaRef(DuaCategory.SUNNAH, "دعاء صلاة الاستخارة"),
        HisnDuaRef(DuaCategory.SUNNAH, "الدعاء بعد التشهد الأخير وقبل السلام"),
        HisnDuaRef(DuaCategory.SUNNAH, "دعاء قنوت الوتر"),
        HisnDuaRef(DuaCategory.ISTIGHFAR, "الاستغفار والتوبة"),
        HisnDuaRef(DuaCategory.TAWBAH, "ما يقول ويفعل من أذنب ذنباً"),
        HisnDuaRef(DuaCategory.DISTRESS, "دعاء الهم والحزن"),
        HisnDuaRef(DuaCategory.DISTRESS, "دعاء الكرب"),
        HisnDuaRef(DuaCategory.DISTRESS, "دعاء من استصعب عليه أمر"),
        HisnDuaRef(DuaCategory.KNOWLEDGE_RIZQ, "الدعاء قضاء الدين"),
        HisnDuaRef(DuaCategory.GUIDANCE, "دعاء الخوف من الشرك"),
        HisnDuaRef(DuaCategory.GUIDANCE, "دعاء من أصابه شك في الإيمان")
    )

    @Volatile
    private var cachedDuas: Map<DuaCategory, List<DuaItem>>? = null

    suspend fun getAllByCategory(): Map<DuaCategory, List<DuaItem>> = withContext(Dispatchers.IO) {
        cachedDuas?.let { return@withContext it }
        quranRepository.ensureInitialized()
        val surahs = quranRepository.getAllSurahs().associateBy { it.index }

        val map = DuaCategory.entries.associateWith { mutableListOf<DuaItem>() }

        // 1. Load Quranic Duas verbatim from verified Tanzil QuranRepository
        for ((idx, ref) in quranDuaRefs.withIndex()) {
            val ayahs = (ref.startAyah..ref.endAyah).mapNotNull { ayahNum ->
                quranRepository.getAyah(ref.surah, ayahNum)
            }
            if (ayahs.isNotEmpty()) {
                val combinedText = ayahs.joinToString(" ۝ ") { it.uthmaniText }
                val surahName = surahs[ref.surah]?.nameArabic ?: "سورة ${ref.surah}"
                val ayahLabel = if (ref.startAyah == ref.endAyah) {
                    "آية ${ref.startAyah}"
                } else {
                    "الآيات ${ref.startAyah}–${ref.endAyah}"
                }
                map.getValue(ref.category).add(
                    DuaItem(
                        id = "q_${ref.category.name}_$idx",
                        category = ref.category,
                        originBadge = DuaOriginBadge.QURAN,
                        title = ref.title,
                        arabicText = combinedText,
                        sourceCitation = "القرآن الكريم — سورة $surahName ($ayahLabel) [نص تنزيل العثماني]"
                    )
                )
            }
        }

        // 2. Load authentic Prophetic Duas from Hisn al-Muslim JSON
        val jsonStr = context.assets.open("adhkar/hisn_almuslim.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonStr)
        for (href in hisnDuaRefs) {
            val chapterObj = root.optJSONObject(href.chapterKey) ?: continue
            val texts = chapterObj.optJSONArray("text") ?: continue
            val footnotes = chapterObj.optJSONArray("footnote")
            for (i in 0 until texts.length()) {
                val text = texts.optString(i, "").trim()
                if (text.isBlank()) continue
                val fn = footnotes?.optString(i, "")?.trim().orEmpty()
                val citation = if (fn.isNotBlank()) {
                    "$fn — (حصن المسلم: ${href.chapterKey})"
                } else {
                    "حصن المسلم — ${href.chapterKey} (القحطاني)"
                }
                map.getValue(href.category).add(
                    DuaItem(
                        id = "h_${href.category.name}_${href.chapterKey}_$i",
                        category = href.category,
                        originBadge = DuaOriginBadge.PROPHETIC_SUNNAH,
                        title = href.chapterKey,
                        arabicText = text,
                        sourceCitation = citation
                    )
                )
            }
        }

        val immutable = map.mapValues { it.value.toList() }
        cachedDuas = immutable
        immutable
    }
}
