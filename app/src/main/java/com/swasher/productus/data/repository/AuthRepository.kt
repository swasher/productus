package com.swasher.productus.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject


class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // вход через Google
    fun signInWithGoogle(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d("AuthRepository", "Starting Google sign in with token")
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        Log.d("AuthRepository", "Успешный вход: uid=${user.uid}, email=${user.email}")
                        saveUserToFirestore(user)
                        onSuccess(user)
                    } else {
                        Log.e("AuthRepository", "Ошибка: пользователь null")
                        onFailure(Exception("Ошибка входа"))
                    }
                } else {
                    Log.e("AuthRepository", "Ошибка входа: ${task.exception?.message}", task.exception)
                    onFailure(task.exception ?: Exception("Неизвестная ошибка входа"))
                }
            }
    }

    // Сохранение пользователя в Firestore
    private fun saveUserToFirestore(user: FirebaseUser) {
        val userRef = firestore.collection("Users").document(user.uid)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                userRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "photoUrl" to (user.photoUrl?.toString() ?: "")
                    )
                )
            }
        }
    }

    // Logout
    fun signOut() {
        auth.signOut()
    }
}
