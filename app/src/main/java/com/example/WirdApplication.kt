package com.example

import android.app.Application
import com.example.audio.RecitationPlayerManager
import com.example.data.adhkar.AdhkarRepository
import com.example.data.duas.DuasRepository
import com.example.data.local.WirdDatabase
import com.example.data.local.WirdPreferences
import com.example.data.quran.QuranRepository
import com.example.data.reciters.ReciterRepository
import com.example.data.tafsir.TafsirRepository
import com.example.qibla.CompassSensorManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(application: Application) {
    val database: WirdDatabase by lazy { WirdDatabase.getInstance(application) }
    val preferences: WirdPreferences by lazy { WirdPreferences(application) }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val quranRepository: QuranRepository by lazy {
        QuranRepository(
            context = application,
            quranDao = database.quranDao(),
            bookmarkDao = database.bookmarkDao()
        )
    }

    val tafsirRepository: TafsirRepository by lazy {
        TafsirRepository(
            tafsirDao = database.tafsirDao(),
            okHttpClient = okHttpClient
        )
    }

    val adhkarRepository: AdhkarRepository by lazy {
        AdhkarRepository(context = application)
    }

    val duasRepository: DuasRepository by lazy {
        DuasRepository(
            context = application,
            quranRepository = quranRepository
        )
    }

    val reciterRepository: ReciterRepository by lazy {
        ReciterRepository(
            context = application,
            reciterDao = database.reciterDao(),
            quranRepository = quranRepository,
            okHttpClient = okHttpClient
        )
    }

    val playerManager: RecitationPlayerManager by lazy {
        RecitationPlayerManager(
            context = application,
            reciterRepository = reciterRepository,
            quranRepository = quranRepository,
            preferences = preferences
        )
    }

    val compassSensorManager: CompassSensorManager by lazy {
        CompassSensorManager(context = application)
    }
}

class WirdApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
