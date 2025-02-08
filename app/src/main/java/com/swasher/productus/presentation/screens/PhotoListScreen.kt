package com.swasher.productus.presentation.screens

import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swasher.productus.data.model.Photo

import com.swasher.productus.presentation.viewmodel.PhotoViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(navController: NavController, viewModel: PhotoViewModel = viewModel()) {
    val photos by viewModel.photos.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPhotos()
//        viewModel.observePhotos()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ваши Фото") }) }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет загруженных фото")
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                items(photos) { photo ->
                    PhotoItem(photo, navController)
                    // было PhotoItem(photo.imageUrl)
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photo: Photo, navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(photo.imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Кнопка для перехода на экран редактирования
        Button(
            onClick = { navController.navigate("photoDetail/${photo.id}") },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Редактировать")
        }

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPhotoListScreen() {
    val fakeNavController = rememberNavController() // Создаём фейковый NavController
    val fakeViewModel = PhotoViewModel() // Создаём фейковый ViewModel

    PhotoListScreen(navController = fakeNavController, viewModel = fakeViewModel)
}