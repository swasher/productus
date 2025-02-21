package com.swasher.productus.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}


// Новый модуль для AuthScope
@Module
@InstallIn(SingletonComponent::class)
object AuthScopeModule {
    @Provides
    @Singleton
    fun provideAuthScope(): AuthScope = AuthScope()
}

class AuthScope {
    private val _isAuthenticated = MutableStateFlow<String?>(null)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    fun setUserId(userId: String?) {
        _isAuthenticated.value = userId
    }
}