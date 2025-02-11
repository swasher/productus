package com.swasher.productus.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.swasher.productus.presentation.viewmodel.PhotoViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: PhotoViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Запрашиваем фокус после появления экрана
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Вариант для поиска через запрос в Firestore
    /*
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchPhotos(searchQuery) // ✅ Асинхронный поиск в Firestore
            Log.d("SearchScreen", "Поиск: $searchQuery")
        } else {
            viewModel.clearSearchResults() // ✅ Очищаем список при пустом вводе
            Log.d("SearchScreen", "Поиск очищен")
        }
    }
    */
    // Вариант для поиска через предварительную коллекцию Firebase
    LaunchedEffect(searchQuery) {
        viewModel.searchPhotos(searchQuery) // ✅ Запускаем поиск локально
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Поиск...") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(searchResults) { photo ->
                PhotoItem(photo, photo.folder, navController)
            }
        }
    }
}
