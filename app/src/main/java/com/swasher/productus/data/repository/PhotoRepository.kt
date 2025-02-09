package com.swasher.productus.data.repository

import android.os.Handler
import android.os.Looper
import com.cloudinary.android.MediaManager
import com.google.firebase.firestore.FirebaseFirestore
import com.swasher.productus.data.model.Photo
import java.util.UUID

class PhotoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val photosCollection = firestore.collection("photos")

    // 📌 Получаем список папок (коллекций)
    fun getFolders(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("Folders")
            .get()
            .addOnSuccessListener { snapshot ->
                val folders = snapshot.documents.map { it.id } // Каждая коллекция - это папка
                onSuccess(folders)
            }
            .addOnFailureListener { onFailure(it) }
    }


    // Получаем фото внутри конкретной папки
    fun getPhotos(folder: String, onSuccess: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("Folders").document(folder).collection("Photos")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                val photos = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
                onSuccess(photos)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Сохраняем фото в конкретную коллекцию (папку)
    fun savePhoto(folder: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val publicId = imageUrl.substringAfterLast("/") // 📌 Извлекаем `vivzby7juh6ph5g4nywq.jpg`
            .substringBeforeLast(".") // 📌 Убираем расширение `.jpg`

        val photo = Photo(
            id = publicId,
            imageUrl = imageUrl,
            comment = "",
            tags = emptyList(),
            createdAt = System.currentTimeMillis()
        )

        firestore.collection("Folders").document(folder).collection("Photos")
            .document(publicId)
            .set(photo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // ???????????? depreciated?
//    // Удаляем всю папку с фото
//    fun deleteFolder(folder: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        val folderRef = firestore.collection("Folders").document(folder).collection("Photos")
//        folderRef.get().addOnSuccessListener { snapshot ->
//            val batch = firestore.batch()
//            snapshot.documents.forEach { batch.delete(it.reference) }
//            batch.commit().addOnSuccessListener {
//                // OLD folderRef.parent?.delete()?.addOnSuccessListener { onSuccess() }
//                firestore.collection("Folders").document(folder).delete()
//                    .addOnSuccessListener { onSuccess() }
//                    .addOnFailureListener { onFailure(it) }
//            }.addOnFailureListener { onFailure(it) }
//        }.addOnFailureListener { onFailure(it) }
//    }

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

//    private fun deletePhotosFromCloudinary(photoUrls: List<String>, onComplete: () -> Unit) {
//        if (photoUrls.isEmpty()) {
//            onComplete() // Если фото нет, просто завершаем
//            return
//        }
//
//        val publicIds = photoUrls.map { it.substringAfterLast("/") } // 📌 Получаем public_id из URL
//            .map { it.substringBeforeLast(".") } // Убираем расширение файла
//
//        val deleteRequests = publicIds.map { publicId ->
//            MediaManager.get().cloudinary.uploader().destroy(publicId, null)
//        }
//
//        // Список для хранения результатов удаления
//        val deleteResults = mutableListOf<Boolean>()
//        val totalRequests = deleteRequests.size
//
//        // Обработка каждого запроса
//        deleteRequests.forEach { request ->
//            request.execute { result ->
//                // Обработка результата
//                if (result != null && result.get("result") == "ok") {
//                    deleteResults.add(true)
//                } else {
//                    deleteResults.add(false)
//                }
//
//                // Проверяем, завершились ли все запросы
//                if (deleteResults.size == totalRequests) {
//                    onComplete() // Завершаем, когда все запросы обработаны
//                }
//            }
//        }
//    }

    fun deletePhotosFromCloudinary(photoUrls: List<String>, onComplete: () -> Unit) {
        if (photoUrls.isEmpty()) {
            onComplete() // Если фото нет, просто завершаем
            return
        }
        val publicIds = photoUrls.map { it.substringAfterLast("/") } // Получаем public_id из URL
            .map { it.substringBeforeLast(".") } // Убираем расширение файла

        // Список для хранения результатов удаления
        val deleteResults = mutableListOf<Boolean>()
        val totalRequests = publicIds.size

        // Создаем и запускаем каждый запрос в отдельном потоке
        publicIds.forEach { publicId ->
            Thread {
                try {
                    val result = MediaManager.get().cloudinary.uploader().destroy(publicId, null)
                    synchronized(deleteResults) {
                        deleteResults.add(result != null && result.get("result") == "ok")
                        // Проверяем, завершились ли все запросы
                        if (deleteResults.size == totalRequests) {
                            // Выполняем callback в главном потоке
                            Handler(Looper.getMainLooper()).post {
                                onComplete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    synchronized(deleteResults) {
                        deleteResults.add(false)
                        if (deleteResults.size == totalRequests) {
                            Handler(Looper.getMainLooper()).post {
                                onComplete()
                            }
                        }
                    }
                }
            }.start()
        }
    }


    // OLD
    //    fun updatePhoto(photoId: String, comment: String, tags: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    //        val updates = mapOf(
    //            "comment" to comment,
    //            "tags" to tags
    //        )
    //
    //        photosCollection.document(photoId)
    //            .update(updates)
    //            .addOnSuccessListener { onSuccess() }
    //            .addOnFailureListener { onFailure(it) }
    //    }
    fun updatePhoto(folder: String, photoId: String, comment: String, tags: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf(
            "comment" to comment,
            "tags" to tags
        )

        FirebaseFirestore.getInstance().collection(folder) // 📌 Используем folder как имя коллекции
            .document(photoId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


}
