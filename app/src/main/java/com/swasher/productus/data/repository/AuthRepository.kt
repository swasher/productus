package com.swasher.productus.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signInWithGoogle(idToken: String, onSuccess: (FirebaseUser) -> Unit, onFailure: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    saveUserToFirestore(user)
                    onSuccess(user)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

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
                firestore.collection("Folders-${user.uid}").document("meta").set(mapOf("createdAt" to System.currentTimeMillis()))
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
