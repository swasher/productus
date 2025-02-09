package com.swasher.productus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

import com.swasher.productus.ui.theme.ProductusTheme
import com.swasher.productus.presentation.camera.CameraActivity
import com.swasher.productus.presentation.screens.FolderScreen
import com.swasher.productus.presentation.screens.PhotoListScreen
import com.swasher.productus.presentation.screens.PhotoDetailScreen
import com.swasher.productus.presentation.viewmodel.PhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProductusTheme {
                val navController = rememberNavController()

                Scaffold(
                    // topBar = { TopAppBar(title = { Text("My Productus Software") }) },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "folders",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("folders") {
                            FolderScreen(navController)
                        }
                        composable("photoList/{folderName}") { backStackEntry ->
                            val folderName = backStackEntry.arguments?.getString("folderName") ?: "Unsorted"
                            PhotoListScreen(navController, folderName)
                        }
                        composable("photoDetail/{folderName}/{photoId}") { backStackEntry ->
                            val folderName = backStackEntry.arguments?.getString("folderName") ?: "Unsorted"
                            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""

                            val viewModel: PhotoViewModel = viewModel(factory = PhotoViewModel.Factory)
                            val photosState = viewModel.photos.collectAsState() // Получаем State<List<Photo>>

                            LaunchedEffect(folderName) {
                                viewModel.loadPhotos(folderName)
                            }

                            val photos = photosState.value // Получаем List<Photo>
                            val photo = photos.find { it.id == photoId } // Получаем фото из списка

                            Log.d("MainActivity", "FolderName: $folderName")
                            Log.d("MainActivity", "PhotoId: $photoId")
                            Log.d("MainActivity", "Photos: $photos")
                            Log.d("MainActivity", "Photo: $photo")

                            if (photo != null) {
                                PhotoDetailScreen(navController, folderName, photo)
                            } else {
                                Text("Фото не найдено")
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Hello Android!", modifier = Modifier.padding(bottom = 16.dp))
        CameraButton() // Добавляем кнопку для открытия CameraActivity
    }
}



@Composable
fun CameraButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            // Создаём Intent для запуска CameraActivity
            val intent = Intent(context, CameraActivity::class.java)
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Открыть камеру")
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ProductusTheme {
        MainScreen()
    }
}