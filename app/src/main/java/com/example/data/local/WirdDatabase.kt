package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkFolderEntity::class,
        BookmarkEntity::class,
        TafsirEntryEntity::class,
        WordMeaningsCacheEntity::class,
        ReciterCoverageEntity::class,
        DownloadedAudioEntity::class,
        TasbihSessionEntity::class,
        WirdLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WirdDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tafsirDao(): TafsirDao
    abstract fun reciterDao(): ReciterDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun wirdLogDao(): WirdLogDao

    companion object {
        @Volatile
        private var INSTANCE: WirdDatabase? = null

        fun getInstance(context: Context): WirdDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WirdDatabase::class.java,
                    "wird_quran_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
