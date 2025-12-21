package com.dripsync.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import com.dripsync.wear.ui.history.HistoryScreen
import com.dripsync.wear.ui.home.HomeScreen
import com.dripsync.wear.ui.settings.SettingsScreen
import com.dripsync.wear.ui.stats.StatsScreen
import com.dripsync.wear.ui.theme.DripSyncWearTheme
import dagger.hilt.android.AndroidEntryPoint

private val BackgroundDark = Color(0xFF0D1520)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DripSyncWearTheme {
                DripSyncPager()
            }
        }
    }
}

@Composable
fun DripSyncPager() {
    val pageCount = 4
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    val pageIndicatorState = object : PageIndicatorState {
        override val pageCount: Int = pageCount
        override val pageOffset: Float = pagerState.currentPageOffsetFraction
        override val selectedPage: Int = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreen()
                1 -> StatsScreen()
                2 -> HistoryScreen()
                3 -> SettingsScreen()
            }
        }

        // ページインジケーター（下部に表示）
        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
    }
}
