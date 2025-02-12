package com.swasher.productus.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.coroutineScope
import com.swasher.productus.data.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

//    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser()) // ❗ Сначала было: authRepository.getCurrentUser()
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = authRepository.getCurrentUser() // ✅ Устанавливаем пользователя при старте
    }

    fun signInWithGoogle(idToken: String) {
        Log.d("AuthViewModel", "signInWithGoogle вызван с idToken: $idToken")

        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken,
                onSuccess = { user ->
                    Log.d("AuthViewModel", "Вход выполнен: ${user.email}") // ✅ Логируем успешный вход
                    _currentUser.value = user
                },
                onFailure = {
                    Log.e("AuthViewModel", "Ошибка входа: ${it.message}", it) // ❌ Логируем ошибку
                    it.printStackTrace()
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
    }
}
