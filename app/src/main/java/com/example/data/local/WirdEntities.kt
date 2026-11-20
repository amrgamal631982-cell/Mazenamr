package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "quran_surahs")
data class SurahEntity(
    @PrimaryKey val index: Int,
    val nameArabic: String,
    val transliteration: String,
    val nameEnglish: String,
    val ayahCount: Int,
    val revelationType: String,
    val revelationOrder: Int,
    val rukuCount: Int,
    val startPage: Int,
    val startJuz: Int
)

@Entity(
    tableName = "quran_ayahs",
    indices = [
        Index(value = ["surahIndex", "ayahNumber"], unique = true),
        Index(value = ["pageNumber"]),
        Index(value = ["juzNumber"]),
        Index(value = ["hizbNumber"])
    ]
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    val surahIndex: Int,
    val ayahNumber: Int,
    val uthmaniText: String,
    val normalizedSearchText: String,
    val bismillahPrefix: String?,
    val juzNumber: Int,
    val hizbNumber: Int,
    val hizbQuarter: Int,
    val pageNumber: Int,
    val sajdaType: String?
)

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["surahIndex", "ayahNumber", "folderId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahIndex: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val surahName: String,
    val uthmaniSnippet: String,
    val folderId: Long = 1L,
    val personalNote: String = "", // Strictly separate from Quran text
    val isReadingPosition: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tafsir_entries",
    primaryKeys = ["sourceId", "surahIndex", "ayahNumber"],
    indices = [Index(value = ["surahIndex", "ayahNumber"])]
)
data class TafsirEntryEntity(
    val sourceId: String,
    val surahIndex: Int,
    val ayahNumber: Int,
    val text: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "word_meanings_cache",
    primaryKeys = ["surahIndex", "ayahNumber"]
)
data class WordMeaningsCacheEntity(
    val surahIndex: Int,
    val ayahNumber: Int,
    val wordsJson: String,
    val sourceLabel: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reciter_coverage",
    primaryKeys = ["reciterId", "surahIndex"]
)
data class ReciterCoverageEntity(
    val reciterId: String,
    val surahIndex: Int,
    val fullSurahAvailable: Boolean,
    val ayahAvailableCount: Int,
    val totalAyahCount: Int,
    val hasTimestamps: Boolean,
    val verifiedAt: Long
)

@Entity(
    tableName = "downloaded_audio",
    primaryKeys = ["reciterId", "surahIndex", "ayahNumber"]
)
data class DownloadedAudioEntity(
    val reciterId: String,
    val surahIndex: Int,
    val ayahNumber: Int, // 0 = full surah file, >0 = individual ayah
    val localPath: String,
    val sizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbih_sessions")
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dhikrText: String,
    val count: Int,
    val target: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wird_logs")
data class WirdLogEntity(
    @PrimaryKey val dateIso: String, // yyyy-MM-dd
    val pagesRead: Int,
    val fromSurah: Int,
    val fromAyah: Int,
    val toSurah: Int,
    val toAyah: Int,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
