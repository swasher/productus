package com.swasher.productus

import FullScreenPhotoScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

import com.swasher.productus.ui.theme.ProductusTheme
import com.swasher.productus.presentation.screens.FolderScreen
import com.swasher.productus.presentation.screens.PhotoListScreen
import com.swasher.productus.presentation.screens.PhotoDetailScreen
import com.swasher.productus.presentation.screens.SearchScreen
import com.swasher.productus.presentation.viewmodel.PhotoViewModel
import com.swasher.productus.presentation.screens.LoginScreen
import com.swasher.productus.presentation.screens.MainTopBar
import com.swasher.productus.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProductusTheme {
                val navController = rememberNavController()

                //val authViewModel: AuthViewModel = viewModel()
                val authViewModel: AuthViewModel = hiltViewModel()  // ✅ Теперь через Hilt

                val currentUser by authViewModel.currentUser.collectAsState()
                val photoViewModel: PhotoViewModel = hiltViewModel() // ✅ Теперь ViewModel не пересоздается

                if (currentUser == null) {
                    LoginScreen(navController)
                } else {

                    Scaffold(
                        topBar = {
                            MainTopBar(navController, authViewModel)
                        },
                    ) { innerPadding ->

                        NavHost(
                            navController = navController,
                            startDestination = "folders",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("loginScreen") {
                                LoginScreen(navController, authViewModel)
                            }
                            composable("folders") {
                                //FolderScreen(navController)
                                FolderScreen(navController) // ✅ hilt: Передаем ViewModel
                            }
                            composable("photoList/{folderName}") { backStackEntry ->
                                val folderName = backStackEntry.arguments?.getString("folderName") ?: "Unsorted"
                                // PhotoListScreen(navController, folderName)
                                PhotoListScreen(navController, folderName, photoViewModel) // ✅ hilt: Передаем ViewModel
                            }
                            composable("photoDetail/{folderName}/{photoId}") { backStackEntry ->
                                val folderName = backStackEntry.arguments?.getString("folderName") ?: "Unsorted"
                                val photoId = backStackEntry.arguments?.getString("photoId") ?: ""

                                LaunchedEffect(folderName) {
                                    photoViewModel.loadPhotos(folderName)
                                }

                                val photos by photoViewModel.photos.collectAsState()

                                if (photos.isEmpty()) {
                                    Text("Фотографии не найдены")
                                } else {

                                    val photo = photos.find { it.id == photoId } // Получаем фото из списка

                                    if (photo != null) {
                                        Log.d("PhotoDetailScreen", "Photo found: $photo")
                                        PhotoDetailScreen(navController, folderName, photo, photoViewModel)
                                    } else {
                                        Text("Фото не найдено")
                                    }
                                }
                            }
                            composable("fullScreenPhoto/{imageUrl}") { backStackEntry ->
                                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                                FullScreenPhotoScreen(navController, imageUrl)
                            }
                            composable("searchScreen") {
                                SearchScreen(navController, photoViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
