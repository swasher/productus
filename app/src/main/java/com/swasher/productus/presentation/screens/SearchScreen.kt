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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.swasher.productus.presentation.viewmodel.PhotoViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: PhotoViewModel = hiltViewModel()) {

    //var searchQuery by remember { mutableStateOf("") }
    val searchQuery by viewModel.searchQuery.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Запрашиваем фокус в поле ввода после появления экрана
    LaunchedEffect(Unit) {
        if (searchQuery.isEmpty()) {
            focusRequester.requestFocus()
        }
    }


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
                        // onValueChange = { searchQuery = it },
                        onValueChange = { query ->
                            viewModel.searchPhotos(query)
                        },
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
                PhotoItem(
                    photo,
                    photo.folder,
                    navController
                )
            }
        }
    }
}
