package com.example.mymusic.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation.NavHost

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MyMusicAppUi() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = "list"
        ) {
            composable("list") {

            }
            composable("detail/{id}") { backStackEntry ->

            }
        }
    }
}
