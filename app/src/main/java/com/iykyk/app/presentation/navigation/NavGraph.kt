package com.iykyk.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.history.HistoryScreen
import com.iykyk.app.presentation.home.HomeScreen
import com.iykyk.app.presentation.people.PeopleBreakdownScreen
import com.iykyk.app.presentation.processing.ProcessingScreen
import com.iykyk.app.presentation.result.CollageResultScreen
import com.iykyk.app.presentation.select.VideoSelectScreen
import com.iykyk.app.presentation.share.ShareScreen

object Destinations {
    const val HOME = "home"
    const val SELECT_VIDEO = "select_video"
    const val PROCESSING = "processing"
    const val COLLAGE_RESULT = "collage_result"
    const val PEOPLE_BREAKDOWN = "people_breakdown"
    const val SHARE = "share"
    const val HISTORY = "history"
}

@Composable
fun IykykNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onCreateCollageClick = {
                    navController.navigate(Destinations.SELECT_VIDEO)
                },
                onOpenCollageClick = {
                    navController.navigate(Destinations.COLLAGE_RESULT)
                },
                onSeeAllClick = {
                    navController.navigate(Destinations.HISTORY)
                }
            )
        }

        composable(Destinations.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onOpenCollageClick = {
                    navController.navigate(Destinations.COLLAGE_RESULT)
                }
            )
        }

        composable(Destinations.SELECT_VIDEO) {
            VideoSelectScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNextClick = {
                    navController.navigate(Destinations.PROCESSING)
                }
            )
        }

        composable(Destinations.PROCESSING) {
            ProcessingScreen(
                viewModel = viewModel,
                onProcessingFinished = {
                    navController.navigate(Destinations.COLLAGE_RESULT) {
                        popUpTo(Destinations.HOME)
                    }
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.COLLAGE_RESULT) {
            CollageResultScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onHomeClick = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onViewPeopleClick = {
                    navController.navigate(Destinations.PEOPLE_BREAKDOWN)
                },
                onShareClick = {
                    navController.navigate(Destinations.SHARE)
                }
            )
        }

        composable(Destinations.PEOPLE_BREAKDOWN) {
            PeopleBreakdownScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SHARE) {
            ShareScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
