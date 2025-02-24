package com.swasher.productus.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.navigation.NavController

import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel

import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


import androidx.lifecycle.viewmodel.compose.viewModel
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.swasher.productus.data.repository.getThumbnailUrl
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(navController: NavController, folderName: String, photo: Photo, viewModel: PhotoViewModel) {
    Log.d("PhotoDetailScreen", "Вход в экран: $photo")

    // val viewModel: PhotoViewModel = hiltViewModel() // теперь viewModel не пересоздается, а берется из контейнера, созданная через hilt

    var comment by remember { mutableStateOf(photo.comment) }
    var tags by remember { mutableStateOf(photo.tags.joinToString(", ")) }
    var name by remember { mutableStateOf(photo.name) }
    var country by remember { mutableStateOf(photo.country) }
    var store by remember { mutableStateOf(photo.store) }
    var price by remember { mutableStateOf(photo.price.toString()) }
    var showDeleteDialog by remember { mutableStateOf(false) } // ✅ Состояние диалога удаления

    val imeInsets = WindowInsets.ime // ✅ Получаем отступ для клавиатуры
    val keyboardPadding = imeInsets.asPaddingValues() // ✅ Преобразуем в PaddingValues

    val screenWidth = LocalConfiguration.current.screenWidthDp // 📌 Получаем ширину экрана в dp

    Log.d("PhotoDetailScreen", "photo.imageUrl: ${photo.imageUrl}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name)
                        IconButton(onClick = { showDeleteDialog = true }) { // ✅ Кнопка удаления
                            Icon(Icons.Default.Delete, contentDescription = "Удалить фото")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(keyboardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val thumbnailUrl = remember { getThumbnailUrl(photo.imageUrl, width = 200, height = 200) }
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .diskCachePolicy(CachePolicy.ENABLED) // 🔥 Включаем кеширование
                    .memoryCachePolicy(CachePolicy.ENABLED) // 🔥 Кешируем в памяти
                    .crossfade(true) // 🔥 Плавное появление изображения
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "Фото",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable {
                        val encodedUrl = URLEncoder.encode(photo.imageUrl, StandardCharsets.UTF_8.toString())
                        navController.navigate("fullScreenPhoto/$encodedUrl")
                    }
            )


            // В PhotoDetailScreen.kt внутри формы редактирования
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Рейтинг:", style = MaterialTheme.typography.bodyMedium)
                RatingBar(
                    rating = photo.rating,
                    onRatingChanged = { newRating ->
                        viewModel.updatePhoto(
                            folder = folderName,
                            photoId = photo.id,
                            comment = photo.comment,
                            tags = photo.tags,
                            name = photo.name,
                            country = photo.country,
                            store = photo.store,
                            price = photo.price,
                            rating = newRating
                        )
                    }
                )
            }


            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Комментарий:") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Теги (через запятую):") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Цена") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = store, onValueChange = { store = it }, label = { Text("Магазин") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Страна") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    viewModel.updatePhoto(folderName, photo.id, comment,  tags.split(","),  name, country, store, price.toFloat(), photo.rating)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

        }
    }

    // ✅ Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить фото?") },
            text = { Text("Вы уверены, что хотите удалить это фото? Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePhoto(folderName, photo.id, photo.imageUrl) // ✅ Удаляем фото
                        navController.popBackStack("photoList/$folderName", inclusive = false) // ✅ Возвращаемся к списку фото
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }


}

// deprecated
//@Preview(showBackground = true)
//@Composable
//fun PreviewPhotoDetailScreen() {
//    PhotoDetailScreen(NavController(LocalContext.current), "DefaultFolder", Photo(id = "1", imageUrl = "https://placehold.co/200"))
//}

@Composable
fun RatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Кнопка сброса
        IconButton(
            onClick = { onRatingChanged(0) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Сбросить рейтинг",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
        }

        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Звезда $i",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChanged(i) },
                tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}
