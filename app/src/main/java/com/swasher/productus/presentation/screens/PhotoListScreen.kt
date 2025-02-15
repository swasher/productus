package com.swasher.productus.presentation.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.hilt.navigation.compose.hiltViewModel
import coil.request.CachePolicy
import coil.request.ImageRequest


import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.getThumbnailUrl
import com.swasher.productus.presentation.camera.CameraActivity
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(navController: NavController, folderName: String, viewModel: PhotoViewModel) {

    val photos by viewModel.filteredPhotos.collectAsState()

    //deprecated val allPhotos by viewModel.photos.collectAsState()

    //val allTags = allPhotos.flatMap { it.tags }.toSet().toList()
    //val allFolders = allPhotos.map { it.folder }.toSet().toList()
    // замена на
    val allTags = photos.flatMap { it.tags }.toSet().toList()
    val allFolders = photos.map { it.folder }.toSet().toList()

    var selectedTag by remember { mutableStateOf<String?>(null) }

    val isUploading by viewModel.isUploading.collectAsState(initial = false)

    val context = LocalContext.current



    // Claude: Add state for LazyListState
    val listState = rememberLazyListState()

    // Claude: Add effect to scroll to top when photos change
    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            listState.animateScrollToItem(index = 0)
        }
    }



    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoPath = result.data?.getStringExtra("photo_path") ?: return@rememberLauncherForActivityResult
            viewModel.uploadPhoto(photoPath, folderName)
        }
    }

    Log.d("PhotoListScreen", "Вход в экран, фолдер: $folderName")

    // Устанавливаем текущую папку
    LaunchedEffect(folderName) {
        viewModel.observePhotos(folderName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName) },
                actions = {
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },

        /*
        floatingActionButton = { // ✅ Добавили кнопку "Добавить фото"
            FloatingActionButton(onClick = {
                val intent = Intent(navController.context, CameraActivity::class.java).apply {
                    putExtra("folderName", folderName) // ✅ Передаём текущую папку в камеру
                }
                navController.context.startActivity(intent)
            }) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Добавить фото")
            }
        }
        */
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, CameraActivity::class.java)
                intent.putExtra("FOLDER_NAME", folderName)
                cameraLauncher.launch(intent)
            }) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Сделать фото")
            }
        }


    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp)
        ) {

            // Индикатор прогресса загрузки в Cloudinary
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Загрузка фото...", modifier = Modifier.align(Alignment.CenterHorizontally))
            }


            // Фильтр по тегам
            LazyRow {
                items(allTags) { tag ->
                    Button(
                        onClick = {
                            selectedTag = if (selectedTag == tag) null else tag
                            viewModel.setFilterTag(selectedTag)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTag == tag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedTag == tag) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (selectedTag == tag) null else BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(tag)
                    }
                }
            }

            if (photos.isEmpty()) {
                Text("Нет загруженных фото", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    state = listState,  // by Claude: Add this line
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoItem(photo, folderName, navController)
                    }
                }
            }
        }
    }
}



@Composable
fun PhotoItem(photo: Photo, folderName: String, navController: NavController) {
    val thumbnailUrl = remember { getThumbnailUrl(photo.imageUrl, width = 200, height = 200) }
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .diskCachePolicy(CachePolicy.ENABLED) // 🔥 Включаем кеширование
            .memoryCachePolicy(CachePolicy.ENABLED) // 🔥 Кешируем в памяти
            .crossfade(true) // 🔥 Плавное появление изображения
            .build()
    )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("PhotoListScreen", "Открываем фото: ${photo.id}") // ✅ Логируем клик
                navController.navigate("photoDetail/$folderName/${photo.id}")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Text(photo.name, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.titleSmall)
            Image(
                //painter = rememberAsyncImagePainter(getThumbnailUrl(photo.imageUrl)),
                // painter = rememberAsyncImagePainter(getThumbnailUrl(photo.imageUrl, width = 200, height = 200)),
                painter = painter,
                contentDescription = "Превью фото",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), // Добавляем отступы
                horizontalArrangement = Arrangement.SpaceBetween // Распределяем пространство между элементами
            ) {
                Text(
                    text = photo.store,
                    style = MaterialTheme.typography.labelSmall // Стиль текста
                )
                Text(
                    text = "${photo.price}€", // Форматируем цену
                    style = MaterialTheme.typography.labelSmall // Стиль текста
                )
            }

        }
    }
}


// DEPRECATED
// @Preview(showBackground = true)
// @Composable
// fun PreviewPhotoListScreen() {
//     val fakeNavController = rememberNavController() // Создаём фейковый NavController
//     val fakeViewModel = PhotoViewModel() // Создаём фейковый ViewModel
//     val folderName = "Тестовая папка" // Пример имени папки
//
//     PhotoListScreen(navController = fakeNavController, folderName, viewModel = fakeViewModel)
// }

