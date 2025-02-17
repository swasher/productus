package com.swasher.productus.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.firebase.auth.FirebaseUser
import com.swasher.productus.data.repository.AuthRepository
import com.swasher.productus.di.AuthScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authScope: AuthScope
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()


    init {
        viewModelScope.launch {
            authRepository.getCurrentUser()?.let { user ->
                onSuccessfulLogin(user)
            }
        }
    }

    private fun onSuccessfulLogin(user: FirebaseUser) {
        _currentUser.value = user
        authScope.setUserId(user.uid)
    }

    // ЧТО  ДЕЛАТЬ С ЭТИМ КОДОМ, КОТОРЫЙ ПОЛУЧАЛ ЮЗЕРА
    // ===============================================

    //  private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser()) // ❗ Сначала было: authRepository.getCurrentUser()
    //  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)

    // private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    // val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    // init {
    //     _currentUser.value = authRepository.getCurrentUser() // ✅ Устанавливаем пользователя при старте
    // }

    // ===============================================




    fun signInWithGoogle(idToken: String) {
        Log.d("AuthViewModel", "signInWithGoogle вызван с idToken: $idToken")

        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken,
                onSuccess = { user ->
                    Log.d("AuthViewModel", "Вход выполнен: ${user.email}") // ✅ Логируем успешный вход
                    onSuccessfulLogin(user)
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
        authScope.setUserId(null)
    }
}
