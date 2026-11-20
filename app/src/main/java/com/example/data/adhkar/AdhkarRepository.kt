package com.example.data.adhkar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class AdhkarCategory(val titleArabic: String, val subtitleArabic: String) {
    MORNING("أذكار الصباح", "تُقرأ من بعد الفجر حتى طلوع الشمس أو الزوال"),
    EVENING("أذكار المساء", "تُقرأ من بعد العصر حتى غروب الشمس أو منتصف الليل"),
    AFTER_PRAYER("أذكار بعد الصلاة", "الأذكار المشروعة بعد السلام من الصلوات المفروضة"),
    SLEEP("أذكار النوم", "الأذكار والأوراد الثابتة قبل النوم"),
    WAKING_UP("أذكار الاستيقاظ", "الأذكار المأثورة عند الاستيقاظ من النوم"),
    MISCELLANEOUS("أذكار متنوعة", "أذكار المنزل والوضوء والمسجد والأذان والسفر والمجلس")
}

data class DhikrItem(
    val id: String,
    val category: AdhkarCategory,
    val chapterTitle: String,
    val arabicText: String,
    val repeatCount: Int,
    val reference: String,
    val explanation: String?
)

class AdhkarRepository(private val context: Context) {

    companion object {
        const val HISN_SOURCE_ATTRIBUTION =
            "كتاب حصن المسلم من أذكار الكتاب والسنة — تأليف: د. سعيد بن علي بن وهف القحطاني"
    }

    @Volatile
    private var cachedByCategory: Map<AdhkarCategory, List<DhikrItem>>? = null

    suspend fun getAllByCategory(): Map<AdhkarCategory, List<DhikrItem>> = withContext(Dispatchers.IO) {
        cachedByCategory?.let { return@withContext it }
        val jsonStr = context.assets.open("adhkar/hisn_almuslim.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonStr)
        val result = AdhkarCategory.entries.associateWith { mutableListOf<DhikrItem>() }

        // 1 & 2. Morning & Evening Adhkar (adapted cleanly for time-specific wording notes while preserving exact text)
        parseChapter(root, "أذكار الصباح والمساء")?.let { items ->
            items.forEachIndexed { idx, pair ->
                val (text, footnote) = pair
                val count = inferRepeatCount(text, footnote)
                val morningAdjusted = adaptMorningEveningText(text, isMorning = true)
                val eveningAdjusted = adaptMorningEveningText(text, isMorning = false)
                result.getValue(AdhkarCategory.MORNING).add(
                    DhikrItem(
                        id = "morning_$idx",
                        category = AdhkarCategory.MORNING,
                        chapterTitle = "أذكار الصباح",
                        arabicText = morningAdjusted,
                        repeatCount = count,
                        reference = buildReference(footnote),
                        explanation = extractExplanation(footnote)
                    )
                )
                result.getValue(AdhkarCategory.EVENING).add(
                    DhikrItem(
                        id = "evening_$idx",
                        category = AdhkarCategory.EVENING,
                        chapterTitle = "أذكار المساء",
                        arabicText = eveningAdjusted,
                        repeatCount = count,
                        reference = buildReference(footnote),
                        explanation = extractExplanation(footnote)
                    )
                )
            }
        }

        // 3. After Prayer
        parseChapter(root, "الأذكار بعد السلام من الصلاة")?.forEachIndexed { idx, (text, footnote) ->
            result.getValue(AdhkarCategory.AFTER_PRAYER).add(
                DhikrItem(
                    id = "prayer_$idx",
                    category = AdhkarCategory.AFTER_PRAYER,
                    chapterTitle = "الأذكار بعد السلام من الصلاة",
                    arabicText = text.trim(),
                    repeatCount = inferRepeatCount(text, footnote),
                    reference = buildReference(footnote),
                    explanation = extractExplanation(footnote)
                )
            )
        }

        // 4. Sleep
        listOf("أذكار النوم", "الدعاء إذا تقلب ليلاً").forEach { chapter ->
            parseChapter(root, chapter)?.forEachIndexed { idx, (text, footnote) ->
                result.getValue(AdhkarCategory.SLEEP).add(
                    DhikrItem(
                        id = "sleep_${chapter}_$idx",
                        category = AdhkarCategory.SLEEP,
                        chapterTitle = chapter,
                        arabicText = text.trim(),
                        repeatCount = inferRepeatCount(text, footnote),
                        reference = buildReference(footnote),
                        explanation = extractExplanation(footnote)
                    )
                )
            }
        }

        // 5. Waking up
        parseChapter(root, "أذكار الاستيقاظ من النوم")?.forEachIndexed { idx, (text, footnote) ->
            result.getValue(AdhkarCategory.WAKING_UP).add(
                DhikrItem(
                    id = "wake_$idx",
                    category = AdhkarCategory.WAKING_UP,
                    chapterTitle = "أذكار الاستيقاظ من النوم",
                    arabicText = text.trim(),
                    repeatCount = inferRepeatCount(text, footnote),
                    reference = buildReference(footnote),
                    explanation = extractExplanation(footnote)
                )
            )
        }

        // 6. Miscellaneous
        val miscChapters = listOf(
            "الذكر قبل الوضوء",
            "الذكر بعد الفراغ من الوضوء",
            "الذكر عند الخروج من المنزل",
            "الذكر عند الدخول المنزل",
            "دعاء الذهاب إلى المسجد",
            "دعاء دخول المسجد",
            "دعاء الخروج من المسجد",
            "أذكار الأذان",
            "دعاء لبس الثوب",
            "دعاء قبل الطعام",
            "دعاء عند الفراغ من الطعام",
            "دعاء ركوب الدابة",
            "دعاء السفر",
            "كفارة المجلس ومايختم به المجالس",
            "فضل التسبيح والتحميد ، والتهليل ، والتكبير"
        )
        miscChapters.forEach { chapter ->
            parseChapter(root, chapter)?.forEachIndexed { idx, (text, footnote) ->
                result.getValue(AdhkarCategory.MISCELLANEOUS).add(
                    DhikrItem(
                        id = "misc_${chapter}_$idx",
                        category = AdhkarCategory.MISCELLANEOUS,
                        chapterTitle = chapter,
                        arabicText = text.trim(),
                        repeatCount = inferRepeatCount(text, footnote),
                        reference = buildReference(footnote),
                        explanation = extractExplanation(footnote)
                    )
                )
            }
        }

        val immutable = result.mapValues { it.value.toList() }
        cachedByCategory = immutable
        immutable
    }

    suspend fun getByCategory(category: AdhkarCategory): List<DhikrItem> =
        getAllByCategory()[category].orEmpty()

    private fun parseChapter(root: JSONObject, chapterTitle: String): List<Pair<String, String>>? {
        val obj = root.optJSONObject(chapterTitle) ?: return null
        val texts = obj.optJSONArray("text") ?: return null
        val footnotes = obj.optJSONArray("footnote")
        val list = mutableListOf<Pair<String, String>>()
        for (i in 0 until texts.length()) {
            val t = texts.optString(i, "").trim()
            if (t.isBlank()) continue
            // Skip pure introductory Khutbah line if footnote index doesn't map
            if (t == "الحمد لله وحده ، والصلاة والسلام على من لا نبي بعده") continue
            val fn = footnotes?.optString(i, "")?.trim().orEmpty()
            list.add(t to fn)
        }
        return list
    }

    private fun adaptMorningEveningText(text: String, isMorning: Boolean): String {
        if (isMorning) return text.trim()
        return text.trim()
            .replace("أصبحنا وأصبح الملك لله", "أمسينا وأمسى الملك لله")
            .replace("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ", "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ")
            .replace("اللهم بك أصبحنا وبك أمسينا", "اللهم بك أمسينا وبك أصبحنا")
            .replace("اللهم إني أصبحت أشهدك", "اللهم إني أمسيت أشهدك")
            .replace("اللهم ما أصبح بي من نعمة", "اللهم ما أمسى بي من نعمة")
            .replace("خير هذا اليوم", "خير هذه الليلة")
    }

    private fun inferRepeatCount(text: String, footnote: String): Int {
        val combined = "$text $footnote"
        return when {
            combined.contains("مائة مرة") || combined.contains("مئة مرة") -> 100
            combined.contains("ثلاثاً وثلاثين") || combined.contains("ثلاثا وثلاثين") -> 33
            combined.contains("عشر مرات") -> 10
            combined.contains("سبع مرات") -> 7
            combined.contains("أربع مرات") -> 4
            combined.contains("ثلاث مرات") || combined.contains("ثلاثاً") || combined.contains("ثلاثا") -> 3
            else -> 1
        }
    }

    private fun buildReference(footnote: String): String {
        return if (footnote.isNotBlank()) {
            "$footnote\n(المصدر: حصن المسلم — القحطاني)"
        } else {
            "المصدر: حصن المسلم — القحطاني"
        }
    }

    private fun extractExplanation(footnote: String): String? {
        if (footnote.isBlank()) return null
        val virtueMarkers = listOf("من قالها", "كفته", "غفرت", "دخل الجنة", "أجير", "تعدل")
        return if (virtueMarkers.any { footnote.contains(it) }) footnote else null
    }
}
