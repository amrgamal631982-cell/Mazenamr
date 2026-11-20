package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.adhkar.AdhkarCategory
import com.example.ui.adhkar.AdhkarDuasScreen
import com.example.ui.adhkar.AdhkarDuasViewModel
import com.example.ui.components.RecitationPlayerBar
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.mushaf.AyahStudyBottomSheet
import com.example.ui.mushaf.MushafIndexScreen
import com.example.ui.mushaf.MushafReaderScreen
import com.example.ui.mushaf.MushafViewModel
import com.example.ui.mushaf.ReaderViewMode
import com.example.ui.navigation.AdhkarDuasRoute
import com.example.ui.navigation.BookmarksRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.MoreToolsRoute
import com.example.ui.navigation.MushafIndexRoute
import com.example.ui.navigation.MushafReaderRoute
import com.example.ui.navigation.QiblaRoute
import com.example.ui.navigation.ReciterDetailRoute
import com.example.ui.navigation.RecitersRoute
import com.example.ui.navigation.SearchRoute
import com.example.ui.navigation.SourcesAuditRoute
import com.example.ui.navigation.StorageManagerRoute
import com.example.ui.navigation.TasbihRoute
import com.example.ui.navigation.WirdTrackerRoute
import com.example.ui.qibla.QiblaScreen
import com.example.ui.qibla.QiblaViewModel
import com.example.ui.reciters.ReciterDetailScreen
import com.example.ui.reciters.RecitersScreen
import com.example.ui.reciters.RecitersViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.WirdTheme
import com.example.ui.tools.BookmarksScreen
import com.example.ui.tools.MoreToolsScreen
import com.example.ui.tools.SearchScreen
import com.example.ui.tools.SourcesAuditScreen
import com.example.ui.tools.StorageManagerScreen
import com.example.ui.tools.TasbihScreen
import com.example.ui.tools.ToolsViewModel
import com.example.ui.tools.WirdTrackerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as WirdApplication).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by appContainer.preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)

            WirdTheme(themeMode = themeMode) {
                WirdRootApp(
                    container = appContainer,
                    themeMode = themeMode,
                    isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                )
            }
        }
    }
}

private data class TopLevelNavItem(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val route: Any
)

@Composable
private fun WirdRootApp(
    container: AppContainer,
    themeMode: AppThemeMode,
    isWideScreen: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
    val mushafViewModel: MushafViewModel = viewModel(factory = MushafViewModel.Factory(container))
    val recitersViewModel: RecitersViewModel = viewModel(factory = RecitersViewModel.Factory(container))
    val adhkarViewModel: AdhkarDuasViewModel = viewModel(factory = AdhkarDuasViewModel.Factory(container))
    val qiblaViewModel: QiblaViewModel = viewModel(factory = QiblaViewModel.Factory(container))
    val toolsViewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(container))

    val playbackState by container.playerManager.state.collectAsStateWithLifecycle()
    val defaultReciterId by recitersViewModel.defaultReciterId.collectAsStateWithLifecycle()

    val topLevelItems = remember {
        listOf(
            TopLevelNavItem("الرئيسية", Icons.Default.Home, "nav_home", HomeRoute),
            TopLevelNavItem("المصحف", Icons.AutoMirrored.Filled.MenuBook, "nav_mushaf", MushafIndexRoute),
            TopLevelNavItem("التلاوات", Icons.Default.GraphicEq, "nav_reciters", RecitersRoute),
            TopLevelNavItem("الأذكار", Icons.Default.Favorite, "nav_adhkar", AdhkarDuasRoute()),
            TopLevelNavItem("المزيد", Icons.Default.Apps, "nav_more", MoreToolsRoute)
        )
    }

    val isTopLevelRoute = currentDest?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<MushafIndexRoute>() ||
            dest.hasRoute<RecitersRoute>() ||
            dest.hasRoute<AdhkarDuasRoute>() ||
            dest.hasRoute<MoreToolsRoute>()
    } ?: true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column {
                RecitationPlayerBar(
                    state = playbackState,
                    onTogglePlayPause = { container.playerManager.togglePlayPause() },
                    onNextAyah = { container.playerManager.nextAyah() },
                    onPreviousAyah = { container.playerManager.previousAyah() },
                    onSpeedChange = { container.playerManager.setPlaybackSpeed(it) },
                    onRepeatTargetChange = { container.playerManager.setSingleAyahRepeat(it) },
                    onSleepTimerChange = { container.playerManager.setSleepTimerMinutes(it) },
                    onSeekTo = { container.playerManager.seekTo(it) },
                    onDismiss = { container.playerManager.stopAndDismiss() }
                )
                if (!isWideScreen && isTopLevelRoute) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelItems.forEach { item ->
                            val selected = when (item.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is MushafIndexRoute -> currentDest?.hasRoute<MushafIndexRoute>() == true
                                is RecitersRoute -> currentDest?.hasRoute<RecitersRoute>() == true
                                is AdhkarDuasRoute -> currentDest?.hasRoute<AdhkarDuasRoute>() == true
                                is MoreToolsRoute -> currentDest?.hasRoute<MoreToolsRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen && isTopLevelRoute) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    topLevelItems.forEach { item ->
                        val selected = when (item.route) {
                            is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                            is MushafIndexRoute -> currentDest?.hasRoute<MushafIndexRoute>() == true
                            is RecitersRoute -> currentDest?.hasRoute<RecitersRoute>() == true
                            is AdhkarDuasRoute -> currentDest?.hasRoute<AdhkarDuasRoute>() == true
                            is MoreToolsRoute -> currentDest?.hasRoute<MoreToolsRoute>() == true
                            else -> false
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                        HomeScreen(
                            uiState = homeState,
                            themeMode = themeMode,
                            onToggleTheme = {
                                val next = if (themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
                                scope.launch { container.preferences.setThemeMode(next) }
                            },
                            onContinueReading = { s, a, p ->
                                navController.navigate(MushafReaderRoute(s, a, p))
                            },
                            onOpenSurah = { s ->
                                navController.navigate(MushafReaderRoute(surahIndex = s, ayahNumber = 1, pageNumber = null))
                            },
                            onOpenAdhkar = { cat ->
                                navController.navigate(AdhkarDuasRoute(initialTab = 0, initialCategoryName = cat.name))
                            },
                            onOpenQibla = { navController.navigate(QiblaRoute) },
                            onOpenTasbih = { navController.navigate(TasbihRoute) },
                            onOpenDuas = {
                                navController.navigate(AdhkarDuasRoute(initialTab = 1))
                            },
                            onOpenWirdTracker = { navController.navigate(WirdTrackerRoute) },
                            onOpenSearch = { navController.navigate(SearchRoute) }
                        )
                    }

                    composable<MushafIndexRoute> {
                        val idxState by mushafViewModel.indexState.collectAsStateWithLifecycle()
                        MushafIndexScreen(
                            uiState = idxState,
                            onSelectTab = mushafViewModel::selectIndexTab,
                            onFilterChange = mushafViewModel::updateIndexFilter,
                            onOpenSurah = { s, startPage ->
                                navController.navigate(MushafReaderRoute(surahIndex = s, ayahNumber = 1, pageNumber = startPage))
                            },
                            onOpenPosition = { s, a, p ->
                                navController.navigate(MushafReaderRoute(s, a, if (p > 0) p else null))
                            },
                            onOpenPage = { page ->
                                navController.navigate(MushafReaderRoute(surahIndex = 1, ayahNumber = 1, pageNumber = page))
                            }
                        )
                    }

                    composable<MushafReaderRoute> { entry ->
                        val args = entry.toRoute<MushafReaderRoute>()
                        LaunchedEffect(args.surahIndex, args.ayahNumber, args.pageNumber) {
                            mushafViewModel.openPosition(args.surahIndex, args.ayahNumber, args.pageNumber)
                        }
                        val readerState by mushafViewModel.readerState.collectAsStateWithLifecycle()
                        val fontSizeSp by mushafViewModel.fontSizeSp.collectAsStateWithLifecycle()
                        val bookmarks by mushafViewModel.bookmarks.collectAsStateWithLifecycle()
                        val folders by mushafViewModel.folders.collectAsStateWithLifecycle()

                        MushafReaderScreen(
                            uiState = readerState,
                            fontSizeSp = fontSizeSp,
                            bookmarks = bookmarks,
                            activePlayingSurah = if (playbackState.isVisible) playbackState.surahIndex else null,
                            activePlayingAyah = if (playbackState.isVisible) playbackState.currentAyah else null,
                            isAudioPlaying = playbackState.isPlaying,
                            onBack = { navController.popBackStack() },
                            onSwitchMode = mushafViewModel::switchViewMode,
                            onPrevPageOrSurah = {
                                if (readerState.viewMode == ReaderViewMode.BY_PAGE) {
                                    if (readerState.currentPage > 1) mushafViewModel.loadPage(readerState.currentPage - 1)
                                } else {
                                    val s = readerState.currentSurah?.index ?: 1
                                    if (s > 1) mushafViewModel.loadSurah(s - 1)
                                }
                            },
                            onNextPageOrSurah = {
                                if (readerState.viewMode == ReaderViewMode.BY_PAGE) {
                                    if (readerState.currentPage < 604) mushafViewModel.loadPage(readerState.currentPage + 1)
                                } else {
                                    val s = readerState.currentSurah?.index ?: 1
                                    if (s < 114) mushafViewModel.loadSurah(s + 1)
                                }
                            },
                            onFontSizeChange = mushafViewModel::adjustFontSize,
                            onPlaySurah = { s ->
                                container.playerManager.playSurah(defaultReciterId, s, 1)
                            },
                            onAyahLongPress = mushafViewModel::onAyahLongPressed
                        )

                        readerState.selectedAyah?.let { selected ->
                            AyahStudyBottomSheet(
                                ayah = selected,
                                surah = readerState.allSurahs[selected.surahIndex],
                                studyBundle = readerState.studyBundle,
                                isLoadingStudy = readerState.isLoadingStudy,
                                isBookmarked = readerState.isSelectedAyahBookmarked,
                                initialNote = readerState.selectedAyahNote,
                                initialFolderId = readerState.selectedAyahFolderId,
                                folders = folders,
                                onDismiss = mushafViewModel::dismissAyahSheet,
                                onPlayAyah = { ayah ->
                                    container.playerManager.playSingleAyah(defaultReciterId, ayah.surahIndex, ayah.ayahNumber)
                                },
                                onRetryFetch = mushafViewModel::refreshSelectedAyahStudy,
                                onSaveBookmark = mushafViewModel::saveAyahBookmark,
                                onRemoveBookmark = mushafViewModel::removeAyahBookmark,
                                onMarkLastRead = mushafViewModel::markAyahAsLastRead
                            )
                        }
                    }

                    composable<RecitersRoute> {
                        val surahs by recitersViewModel.surahs.collectAsStateWithLifecycle()
                        val listeningPos by recitersViewModel.listeningPosition.collectAsStateWithLifecycle()
                        RecitersScreen(
                            reciters = recitersViewModel.reciters,
                            defaultReciterId = defaultReciterId,
                            listeningPosition = listeningPos,
                            surahs = surahs,
                            onOpenReciter = { id -> navController.navigate(ReciterDetailRoute(id)) },
                            onResumeListening = { recId, s, a ->
                                container.playerManager.playSurah(recId, s, a)
                            }
                        )
                    }

                    composable<ReciterDetailRoute> { entry ->
                        val args = entry.toRoute<ReciterDetailRoute>()
                        val surahs by recitersViewModel.surahs.collectAsStateWithLifecycle()
                        val coverageMap by remember(args.reciterId) {
                            recitersViewModel.observeCoverageFor(args.reciterId)
                        }.collectAsStateWithLifecycle()
                        val downloadedAudio by recitersViewModel.downloadedAudio.collectAsStateWithLifecycle()
                        val verifyProgress by recitersViewModel.coverageVerifyProgress.collectAsStateWithLifecycle()
                        val downloadingSurahs by recitersViewModel.downloadingSurah.collectAsStateWithLifecycle()
                        val statusMsg by recitersViewModel.statusMessage.collectAsStateWithLifecycle()

                        ReciterDetailScreen(
                            reciterId = args.reciterId,
                            isDefaultReciter = defaultReciterId == args.reciterId,
                            surahs = surahs,
                            coverageMap = coverageMap,
                            downloadedAudio = downloadedAudio,
                            verifyProgress = verifyProgress,
                            downloadingSurahs = downloadingSurahs,
                            statusMessage = statusMsg,
                            onBack = { navController.popBackStack() },
                            onSetDefault = { recitersViewModel.setDefaultReciter(args.reciterId) },
                            onVerifyAll = { recitersViewModel.verifyAllSurahs(args.reciterId) },
                            onVerifySurah = { s -> recitersViewModel.verifySingleSurah(args.reciterId, s) },
                            onPlaySurah = { s -> container.playerManager.playSurah(args.reciterId, s, 1) },
                            onPlayAyahRange = { s, from, to, repeat ->
                                container.playerManager.playAyahRange(args.reciterId, s, from, to, repeat)
                            },
                            onDownloadSurah = { s -> recitersViewModel.downloadSurahForOffline(args.reciterId, s) },
                            onDismissStatus = recitersViewModel::clearStatusMessage
                        )
                    }

                    composable<AdhkarDuasRoute> { entry ->
                        val args = entry.toRoute<AdhkarDuasRoute>()
                        LaunchedEffect(args.initialTab, args.initialCategoryName) {
                            adhkarViewModel.selectTab(args.initialTab)
                            args.initialCategoryName?.let { name ->
                                runCatching { AdhkarCategory.valueOf(name) }.getOrNull()?.let {
                                    adhkarViewModel.selectAdhkarCategory(it)
                                }
                            }
                        }
                        val uiState by adhkarViewModel.uiState.collectAsStateWithLifecycle()
                        AdhkarDuasScreen(
                            uiState = uiState,
                            onSelectTab = adhkarViewModel::selectTab,
                            onSelectAdhkarCategory = adhkarViewModel::selectAdhkarCategory,
                            onSelectDuaCategory = adhkarViewModel::selectDuaCategory,
                            onDecrementDhikr = adhkarViewModel::decrementDhikr,
                            onResetDhikr = adhkarViewModel::resetDhikr,
                            onResetCategory = adhkarViewModel::resetCategoryCounters
                        )
                    }

                    composable<MoreToolsRoute> {
                        MoreToolsScreen(
                            themeMode = themeMode,
                            onSelectTheme = { mode -> scope.launch { container.preferences.setThemeMode(mode) } },
                            onOpenQibla = { navController.navigate(QiblaRoute) },
                            onOpenTasbih = { navController.navigate(TasbihRoute) },
                            onOpenWird = { navController.navigate(WirdTrackerRoute) },
                            onOpenSearch = { navController.navigate(SearchRoute) },
                            onOpenBookmarks = { navController.navigate(BookmarksRoute) },
                            onOpenStorage = { navController.navigate(StorageManagerRoute) },
                            onOpenSourcesAudit = { navController.navigate(SourcesAuditRoute) }
                        )
                    }

                    composable<QiblaRoute> {
                        val qiblaState by qiblaViewModel.uiState.collectAsStateWithLifecycle()
                        QiblaScreen(
                            uiState = qiblaState,
                            onRequestLocation = { qiblaViewModel.requestCurrentLocation(context) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<TasbihRoute> {
                        val cfg by toolsViewModel.tasbihConfig.collectAsStateWithLifecycle()
                        val sessions by toolsViewModel.tasbihSessions.collectAsStateWithLifecycle()
                        val totalAll by toolsViewModel.totalTasbihAllTime.collectAsStateWithLifecycle()
                        TasbihScreen(
                            config = cfg,
                            sessions = sessions,
                            totalAllTime = totalAll,
                            onIncrement = toolsViewModel::incrementTasbih,
                            onResetAndSave = toolsViewModel::resetTasbihAndSaveSession,
                            onUpdateSettings = toolsViewModel::updateTasbihSettings,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<WirdTrackerRoute> {
                        val plan by toolsViewModel.wirdPlan.collectAsStateWithLifecycle()
                        val logs by toolsViewModel.wirdLogs.collectAsStateWithLifecycle()
                        val pos by toolsViewModel.readingPosition.collectAsStateWithLifecycle()
                        WirdTrackerScreen(
                            plan = plan,
                            logs = logs,
                            readingPosition = pos,
                            onUpdatePlan = { dPages, kDays, rem, hr, min ->
                                toolsViewModel.updateWirdPlan(context, dPages, kDays, rem, hr, min)
                            },
                            onLogPages = toolsViewModel::logTodayPages,
                            onContinueReading = { s, a, p ->
                                navController.navigate(MushafReaderRoute(s, a, p))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<SearchRoute> {
                        val q by toolsViewModel.searchQuery.collectAsStateWithLifecycle()
                        val results by toolsViewModel.searchResults.collectAsStateWithLifecycle()
                        val searching by toolsViewModel.isSearching.collectAsStateWithLifecycle()
                        SearchScreen(
                            query = q,
                            results = results,
                            isSearching = searching,
                            onQueryChange = toolsViewModel::updateSearchQuery,
                            onOpenResult = { s, a, p ->
                                navController.navigate(MushafReaderRoute(s, a, p))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<BookmarksRoute> {
                        val folders by toolsViewModel.folders.collectAsStateWithLifecycle()
                        val bookmarks by toolsViewModel.bookmarks.collectAsStateWithLifecycle()
                        BookmarksScreen(
                            folders = folders,
                            bookmarks = bookmarks,
                            onCreateFolder = toolsViewModel::createFolder,
                            onDeleteBookmark = toolsViewModel::deleteBookmark,
                            onOpenBookmark = { s, a, p ->
                                navController.navigate(MushafReaderRoute(s, a, p))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<StorageManagerRoute> {
                        val packs by toolsViewModel.tafsirPacks.collectAsStateWithLifecycle()
                        val wbwBytes by toolsViewModel.wordMeaningsBytes.collectAsStateWithLifecycle()
                        val dlProgress by toolsViewModel.packDownloadProgress.collectAsStateWithLifecycle()
                        val msg by toolsViewModel.storageMessage.collectAsStateWithLifecycle()
                        val audioFiles by toolsViewModel.downloadedAudio.collectAsStateWithLifecycle()
                        val audioBytes by toolsViewModel.totalAudioBytes.collectAsStateWithLifecycle()
                        StorageManagerScreen(
                            tafsirPacks = packs,
                            wordMeaningsBytes = wbwBytes,
                            downloadProgress = dlProgress,
                            storageMessage = msg,
                            downloadedAudio = audioFiles,
                            totalAudioBytes = audioBytes,
                            onRefresh = toolsViewModel::refreshStorageStats,
                            onDownloadPack = toolsViewModel::downloadTafsirPack,
                            onDeletePack = toolsViewModel::deleteTafsirPack,
                            onClearWordMeanings = toolsViewModel::clearWordMeaningsCache,
                            onDeleteAudioFile = toolsViewModel::deleteAudioFile,
                            onDeleteAllAudio = toolsViewModel::deleteAllAudio,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<SourcesAuditRoute> {
                        val report by toolsViewModel.auditReport.collectAsStateWithLifecycle()
                        val auditing by toolsViewModel.isAuditing.collectAsStateWithLifecycle()
                        SourcesAuditScreen(
                            auditReport = report,
                            isAuditing = auditing,
                            onRunAudit = toolsViewModel::runQuranAudit,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
