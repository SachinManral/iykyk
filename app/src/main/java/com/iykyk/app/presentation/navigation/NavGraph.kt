package com.iykyk.app.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.home.HomeScreen
import com.iykyk.app.presentation.people.PeopleBreakdownScreen
import com.iykyk.app.presentation.processing.ProcessingScreen
import com.iykyk.app.presentation.result.CollageResultScreen
import com.iykyk.app.presentation.select.VideoSelectScreen

object Destinations {
    const val HOME = "home"
    const val SELECT_VIDEO = "select_video"
    const val PROCESSING = "processing"
    const val COLLAGE_RESULT = "collage_result"
    const val PEOPLE_BREAKDOWN = "people_breakdown"
}

@Composable
fun IykykNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
        }
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onCreateCollageClick = {
                    navController.navigate(Destinations.SELECT_VIDEO)
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
                    if (!navController.popBackStack(Destinations.HOME, inclusive = false)) {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onHomeClick = {
                    if (!navController.popBackStack(Destinations.HOME, inclusive = false)) {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onViewPeopleClick = {
                    navController.navigate(Destinations.PEOPLE_BREAKDOWN)
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
    }
}



