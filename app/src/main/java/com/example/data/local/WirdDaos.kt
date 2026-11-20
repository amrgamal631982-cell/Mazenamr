package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Strictly read-only Quran DAO for application queries.
 * Seeding happens once inside a verified transaction; no update or delete methods exist on Quran text.
 */
@Dao
interface QuranDao {
    @Query("SELECT COUNT(*) FROM quran_surahs")
    suspend fun getSurahCount(): Int

    @Query("SELECT COUNT(*) FROM quran_ayahs")
    suspend fun getAyahCount(): Int

    @Query("SELECT * FROM quran_surahs ORDER BY `index` ASC")
    suspend fun getAllSurahs(): List<SurahEntity>

    @Query("SELECT * FROM quran_surahs WHERE `index` = :surahIndex LIMIT 1")
    suspend fun getSurah(surahIndex: Int): SurahEntity?

    @Query("SELECT * FROM quran_ayahs WHERE surahIndex = :surahIndex ORDER BY ayahNumber ASC")
    suspend fun getAyahsBySurah(surahIndex: Int): List<AyahEntity>

    @Query("SELECT * FROM quran_ayahs WHERE pageNumber = :pageNumber ORDER BY id ASC")
    suspend fun getAyahsByPage(pageNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM quran_ayahs WHERE juzNumber = :juzNumber ORDER BY id ASC")
    suspend fun getAyahsByJuz(juzNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM quran_ayahs WHERE hizbNumber = :hizbNumber ORDER BY id ASC")
    suspend fun getAyahsByHizb(hizbNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM quran_ayahs WHERE surahIndex = :surahIndex AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getAyah(surahIndex: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM quran_ayahs ORDER BY id ASC")
    suspend fun getAllAyahsOrdered(): List<AyahEntity>

    @Query(
        "SELECT * FROM quran_ayahs WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%' ORDER BY id ASC LIMIT :limit"
    )
    suspend fun searchAyahsNormalized(normalizedQuery: String, limit: Int = 120): List<AyahEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun seedSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun seedAyahs(ayahs: List<AyahEntity>)

    @Transaction
    suspend fun seedQuranOnce(surahs: List<SurahEntity>, ayahs: List<AyahEntity>) {
        if (getSurahCount() == 0 && getAyahCount() == 0) {
            seedSurahs(surahs)
            seedAyahs(ayahs)
        }
    }
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark_folders ORDER BY id ASC")
    fun observeFolders(): Flow<List<BookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE surahIndex = :surahIndex AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun findBookmark(surahIndex: Int, ayahNumber: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE surahIndex = :surahIndex AND ayahNumber = :ayahNumber")
    suspend fun deleteByAyah(surahIndex: Int, ayahNumber: Int)
}

@Dao
interface TafsirDao {
    @Query("SELECT * FROM tafsir_entries WHERE sourceId = :sourceId AND surahIndex = :surahIndex AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getEntry(sourceId: String, surahIndex: Int, ayahNumber: Int): TafsirEntryEntity?

    @Query("SELECT * FROM tafsir_entries WHERE surahIndex = :surahIndex AND ayahNumber = :ayahNumber")
    suspend fun getEntriesForAyah(surahIndex: Int, ayahNumber: Int): List<TafsirEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TafsirEntryEntity>)

    @Query("SELECT COUNT(*) FROM tafsir_entries WHERE sourceId = :sourceId")
    suspend fun countEntriesForSource(sourceId: String): Int

    @Query("SELECT COALESCE(SUM(LENGTH(text) * 2), 0) FROM tafsir_entries WHERE sourceId = :sourceId")
    suspend fun estimateBytesForSource(sourceId: String): Long

    @Query("SELECT COUNT(DISTINCT surahIndex) FROM tafsir_entries WHERE sourceId = :sourceId")
    suspend fun countSurahsForSource(sourceId: String): Int

    @Query("DELETE FROM tafsir_entries WHERE sourceId = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("SELECT * FROM word_meanings_cache WHERE surahIndex = :surahIndex AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getWordMeanings(surahIndex: Int, ayahNumber: Int): WordMeaningsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordMeanings(item: WordMeaningsCacheEntity)

    @Query("SELECT COALESCE(SUM(LENGTH(wordsJson) * 2), 0) FROM word_meanings_cache")
    suspend fun estimateWordMeaningsBytes(): Long

    @Query("DELETE FROM word_meanings_cache")
    suspend fun clearWordMeanings()
}

@Dao
interface ReciterDao {
    @Query("SELECT * FROM reciter_coverage WHERE reciterId = :reciterId ORDER BY surahIndex ASC")
    fun observeCoverage(reciterId: String): Flow<List<ReciterCoverageEntity>>

    @Query("SELECT * FROM reciter_coverage WHERE reciterId = :reciterId AND surahIndex = :surahIndex LIMIT 1")
    suspend fun getCoverage(reciterId: String, surahIndex: Int): ReciterCoverageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoverage(items: List<ReciterCoverageEntity>)

    @Query("SELECT * FROM downloaded_audio ORDER BY downloadedAt DESC")
    fun observeDownloadedAudio(): Flow<List<DownloadedAudioEntity>>

    @Query("SELECT * FROM downloaded_audio WHERE reciterId = :reciterId AND surahIndex = :surahIndex AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getDownloadedAudio(reciterId: String, surahIndex: Int, ayahNumber: Int): DownloadedAudioEntity?

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM downloaded_audio")
    fun observeTotalAudioBytes(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedAudio(item: DownloadedAudioEntity)

    @Query("DELETE FROM downloaded_audio WHERE reciterId = :reciterId AND surahIndex = :surahIndex AND ayahNumber = :ayahNumber")
    suspend fun deleteDownloadedAudio(reciterId: String, surahIndex: Int, ayahNumber: Int)

    @Query("DELETE FROM downloaded_audio WHERE reciterId = :reciterId")
    suspend fun deleteAllForReciter(reciterId: String)

    @Query("DELETE FROM downloaded_audio")
    suspend fun deleteAllAudio()
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_sessions ORDER BY timestamp DESC LIMIT 100")
    fun observeSessions(): Flow<List<TasbihSessionEntity>>

    @Query("SELECT COALESCE(SUM(count), 0) FROM tasbih_sessions")
    fun observeTotalCount(): Flow<Long>

    @Insert
    suspend fun insertSession(session: TasbihSessionEntity)

    @Query("DELETE FROM tasbih_sessions")
    suspend fun clearAll()
}

@Dao
interface WirdLogDao {
    @Query("SELECT * FROM wird_logs ORDER BY dateIso DESC")
    fun observeLogs(): Flow<List<WirdLogEntity>>

    @Query("SELECT * FROM wird_logs WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getLogByDate(dateIso: String): WirdLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: WirdLogEntity)
}
