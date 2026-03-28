package me.bmax.apatch.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.theme.LocalMainPagerState

@Destination<RootGraph>(start = true)
@Composable
fun MainScreen(navigator: DestinationsNavigator) {
    val mainPagerState = LocalMainPagerState.current ?: return

    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    HorizontalPager(
        state = mainPagerState.pagerState,
        beyondViewportPageCount = 4,
        userScrollEnabled = true,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (BottomBarDestination.entries.getOrNull(page)) {
            BottomBarDestination.Home -> HomeScreen(navigator)
            BottomBarDestination.KModule -> KPModuleScreen(navigator)
            BottomBarDestination.SuperUser -> SuperUserScreen()
            BottomBarDestination.AModule -> APModuleScreen(navigator)
            BottomBarDestination.Settings -> SettingScreen(navigator)
            null -> {}
        }
    }
}
