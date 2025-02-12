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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        Log.d("AuthViewModel", "signInWithGoogle вызван с idToken: $idToken")

        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken,
                onSuccess = {
                    Log.d("AuthViewModel", "Вход выполнен: ${it.email}") // ✅ Логируем успешный вход
                    _currentUser.value = it
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
