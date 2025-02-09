package com.swasher.productus.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image

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

import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(navController: NavController, folderName: String, photo: Photo, viewModel: PhotoViewModel = viewModel()) {

    var comment by remember { mutableStateOf(photo.comment) }
    var tags by remember { mutableStateOf(photo.tags.joinToString(", ")) }
    var showDeleteDialog by remember { mutableStateOf(false) } // ✅ Состояние диалога удаления

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(folderName)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(photo.imageUrl),
                contentDescription = "Фото",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text("Категория: $folderName")

            Text("Комментарий:")
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Теги (через запятую):")
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.updatePhoto(folderName, photo.id, comment, tags.split(",").map { it.trim() })
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

@Preview(showBackground = true)
@Composable
fun PreviewPhotoDetailScreen() {
    PhotoDetailScreen(NavController(LocalContext.current), "DefaultFolder", Photo(id = "1", imageUrl = "https://placehold.co/200"))
}
