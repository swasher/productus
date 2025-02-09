package com.swasher.productus.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
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
        Log.d("PhotoRepository", "Fetching photos for folder: $folder")
        firestore.collection("Folders").document(folder).collection("Photos")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("PhotoRepository", "Получено документов: ${snapshot.size()}")
                val photos = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
                Log.d("PhotoRepository", "Преобразовано фото: ${photos.size}")
                onSuccess(photos)
            }
            .addOnFailureListener {
                Log.e("PhotoRepository", "Ошибка загрузки фото: ${it.message}")
                onFailure(it)
            }
    }

    // Сохраняем фото в конкретную коллекцию (папку)
    fun savePhoto(folder: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val publicId = imageUrl.substringAfterLast("/") // 📌 Извлекаем `vivzby7juh6ph5g4nywq.jpg`
            .substringBeforeLast(".") // 📌 Убираем расширение `.jpg`

        val photo = Photo(
            id = publicId,
            imageUrl = imageUrl,
            folder = folder,
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



    fun observePhotos(folder: String, onUpdate: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("Folders").document(folder).collection("Photos")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val photos = snapshot?.documents?.mapNotNull { it.toObject(Photo::class.java) } ?: emptyList()
                onUpdate(photos)
            }
    }




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


    fun deletePhoto(folder: String, photoId: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("Folders").document(folder).collection("Photos")
            .document(photoId)
            .delete()
            .addOnSuccessListener {
                // ✅ Удаляем фото из Cloudinary
                val publicId = imageUrl.substringAfterLast("/").substringBeforeLast(".")

                Log.d("PhotoRepository", "Extracted publicId: $publicId from URL: $imageUrl")


                // if only for debug
                if (publicId.isEmpty()) {
                    Log.e("PhotoRepository", "Invalid publicId extracted from URL: $imageUrl")
                    onFailure(Exception("Invalid publicId"))
                }


                try {
                    val result = MediaManager.get().cloudinary.uploader().destroy(publicId, null)
                    // Более раскрытая версия для дебага, но можно и так оставить, если работает
                    //                    Log.d("PhotoRepository", "Фото удалено из Cloudinary: $publicId, result: $result")
                    //                    onSuccess()

                    if (result == null || result["result"] != "ok") {
                        Log.e("PhotoRepository", "Cloudinary deletion failed: $result")
                        onFailure(Exception("Cloudinary deletion failed"))
                    } else {
                        Log.d("PhotoRepository", "Фото удалено из Cloudinary: $publicId, result: $result")
                        onSuccess()
                    }

                } catch (e: Exception) {
                    Log.e("PhotoRepository", "Ошибка удаления из Cloudinary: ${e.message}")
                    onFailure(e)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }


    fun updatePhoto(folder: String, photoId: String, comment: String, tags: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf(
            "comment" to comment,
            "tags" to tags
        )

        firestore.collection("Folders").document(folder).collection("Photos")
            .document(photoId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

}
