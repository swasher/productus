package com.swasher.productus.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.swasher.productus.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(navController: NavController, viewModel: AuthViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()


    if (currentUser == null) return // Если пользователь не залогинен – не показываем TopBar

    TopAppBar(
        title = { Text("Productus") },
        actions = {

            // Добавляем иконку поиска
            IconButton(
                onClick = { navController.navigate("searchScreen") }
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Поиск"
                )
            }

            currentUser?.photoUrl?.toString()?.let { photoUrl ->
                var expanded by remember { mutableStateOf(false) }
                val email = currentUser?.email ?: "Not logged in"

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUrl),
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Другие пункты меню
                        DropdownMenuItem(
                            text = { Text(email) },
                            onClick = {
                                expanded = false
                            }
                        )

                        // Горизонтальная черта
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )

                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                expanded = false
                                viewModel.signOut()
                                navController.navigate("loginScreen") {
                                    popUpTo("folders") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}
