package com.swasher.productus.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.swasher.productus.data.model.Photo
import java.util.UUID

class PhotoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val photosCollection = firestore.collection("photos")

    fun savePhoto(imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val photo = Photo(
            id = UUID.randomUUID().toString(),
            imageUrl = imageUrl,
            comment = "",
            tags = emptyList(),
            folder = "Unsorted",
            createdAt = System.currentTimeMillis()
        )

        photosCollection.document(photo.id)
            .set(photo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getPhotos(onSuccess: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        photosCollection.orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                val photos = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
                onSuccess(photos)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun observePhotos(onUpdate: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        photosCollection.orderBy("createdAt").addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error)
                return@addSnapshotListener
            }

            val photos = snapshot?.documents?.mapNotNull { it.toObject(Photo::class.java) } ?: emptyList()
            onUpdate(photos)
        }
    }

    fun updatePhoto(photoId: String, comment: String, tags: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf(
            "comment" to comment,
            "tags" to tags
        )

        photosCollection.document(photoId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


}
