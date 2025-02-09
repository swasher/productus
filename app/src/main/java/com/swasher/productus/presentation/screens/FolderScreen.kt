package com.swasher.productus.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(navController: NavController, viewModel: PhotoViewModel = viewModel())  {
    val folders by viewModel.folders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // 📌 Загружаем список папок при запуске экрана
    LaunchedEffect(Unit) {
        viewModel.loadFolders()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Разделы") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить папку")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders) { folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("photoList/$folder") },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(
                        text = folder,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Создать папку") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Название папки") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.createFolder(newFolderName)
                        showDialog = false
                    }) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFolderScreen() {
    FolderScreen(navController = rememberNavController())
}
