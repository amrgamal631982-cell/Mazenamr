package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object MushafIndexRoute

@Serializable
data class MushafReaderRoute(
    val surahIndex: Int = 1,
    val ayahNumber: Int = 1,
    val pageNumber: Int? = null
)

@Serializable
object RecitersRoute

@Serializable
data class ReciterDetailRoute(
    val reciterId: String
)

@Serializable
data class AdhkarDuasRoute(
    val initialTab: Int = 0, // 0 = Adhkar, 1 = Duas
    val initialCategoryName: String? = null
)

@Serializable
object MoreToolsRoute

@Serializable
object QiblaRoute

@Serializable
object TasbihRoute

@Serializable
object WirdTrackerRoute

@Serializable
object SearchRoute

@Serializable
object BookmarksRoute

@Serializable
object StorageManagerRoute

@Serializable
object SourcesAuditRoute
