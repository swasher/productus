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
import androidx.compose.material.icons.filled.ArrowBack

import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(navController: NavController, photo: Photo, viewModel: PhotoViewModel = viewModel()) {

    // look as depreciated
    val repository = remember { PhotoRepository() }

    var comment by remember { mutableStateOf(photo.comment) }
    var tags by remember { mutableStateOf(photo.tags.joinToString(", ")) }

    // look as depreciated
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование фото") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
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

            Text("Комментарий:")
//            BasicTextField(
//                value = comment,
//                onValueChange = { comment = it },
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
//                keyboardActions = KeyboardActions(onDone = { /* Скрыть клавиатуру */ }),
//                modifier = Modifier.fillMaxWidth()
//            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Теги (через запятую):")
//            BasicTextField(
//                value = tags,
//                onValueChange = { tags = it },
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
//                keyboardActions = KeyboardActions(onDone = { /* Скрыть клавиатуру */ }),
//                modifier = Modifier.fillMaxWidth()
//            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.updatePhoto(photo.id, comment, tags.split(",").map { it.trim() })
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPhotoDetailScreen() {
    PhotoDetailScreen(NavController(LocalContext.current), Photo(id = "1", imageUrl = "https://placehold.co/200"))
}
