package com.swasher.productus.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.swasher.productus.presentation.viewmodel.PhotoViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(navController: NavController)  {

    val viewModel: PhotoViewModel = hiltViewModel() // use hilt!

    val folders by viewModel.folders.collectAsState()
    var folderToRename by remember { mutableStateOf<String?>(null) } // ✅ Выбранная папка для переименования
    var newFolderName by remember { mutableStateOf("") } // ✅ Новое имя папки
    var showRenameDialog by remember { mutableStateOf(false) } // ✅ Состояние диалога
    var folderToDelete by remember { mutableStateOf<String?>(null) } // ✅ Состояние выбранной папки
    var showDeleteDialog by remember { mutableStateOf(false) } // ✅ Состояние диалога удаления категории
    var showNewFolderDialog by remember { mutableStateOf(false) }    // dialog для создания категории
    var isDeleting by remember { mutableStateOf(false) }
    val folderCounts by viewModel.folderCounts.collectAsState()

    // 📌 Загружаем список папок при запуске экрана
    LaunchedEffect(Unit) {
        viewModel.loadFolders()
        viewModel.loadFolderCounts()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Категории") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewFolderDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить папку")
            }
        }
    ) { padding ->


        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (folders.isEmpty()) {
                Text("Нет созданных папок", modifier = Modifier.padding(16.dp))

            } else {

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("photoList/$folder") }
                                .padding(8.dp), // Отступы вокруг карточки
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Тень для карточки
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp), // Отступы внутри карточки
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ){
                                    Text(text = folder)
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "${folderCounts[folder] ?: 0}",
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        folderToRename = folder
                                        newFolderName = folder
                                        showRenameDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Переименовать папку")
                                    }
                                    IconButton(onClick = {
                                        folderToDelete = folder
                                        showDeleteDialog = true
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить папку")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



        // ✅ Диалог создания папки
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("Создать папку") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {  newFolderName = it.trim() },
                        label = { Text("Название папки") },
                        singleLine = true, // Запрещаем многострочный ввод
                        maxLines = 1 // Ограничиваем количество строк
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.createFolder(newFolderName)
                        showNewFolderDialog = false
                    }) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    Button(onClick = { showNewFolderDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }


        // ✅ Диалог переименования папки
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Переименовать папку") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Новое название") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            folderToRename?.let { oldName ->
                                viewModel.renameFolder(
                                    oldName,
                                    newFolderName
                                )
                            }
                            showRenameDialog = false
                        }
                    ) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    Button(onClick = { showRenameDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // ✅ Диалог подтверждения удаления папки
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить папку?") },
                // text = { Text("Вы уверены, что хотите удалить эту папку и все её фото? Это действие нельзя отменить.") },
                text = {
                    Column {
                        Text("Вы уверены, что хотите удалить эту папку и все её фото? Это действие нельзя отменить.")
                        if (isDeleting) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            folderToDelete?.let { folder ->
                                viewModel.deleteFolder(
                                    folder,
                                    onSuccess = {
                                        isDeleting = false
                                        showDeleteDialog = false
                                        navController.popBackStack("folders", inclusive = false) // ✅ Обновляем список
                                    },
                                    onFailure = {
                                        isDeleting = false
                                        showDeleteDialog = false
                                    })
                            }
                        },
                        enabled = !isDeleting
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
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
