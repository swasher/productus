/*
Отвечает за работу с данными. Работает с Firebase и Cloudinary. Он не знает про UI.

✅ Достаёт данные из Firestore
✅ Загружает фото в Cloudinary
✅ Обновляет Firestore
✅ Удаляет фото из Cloudinary
*/

package com.swasher.productus.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.swasher.productus.data.model.Photo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

fun getThumbnailUrl(imageUrl: String, width: Int = 200, height: Int = 200): String {
    // c_auto - автоматески подгоняет под размер
    // g_auto - gravity, в центрирует по сюжету
    return imageUrl.replace("/upload/", "/upload/w_${width},h_${height},c_auto,g_auto/")
}


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
    // СОХРАНЕНИЕ используется для новой фотки! (для обновления есть updatePhoto)
    fun savePhoto(
        folder: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val publicId = imageUrl.substringAfterLast("/") // 📌 Извлекаем `vivzby7juh6ph5g4nywq.jpg`
            .substringBeforeLast(".") // 📌 Убираем расширение `.jpg`

        val photo = Photo(
            id = publicId,
            imageUrl = imageUrl,
            folder = folder,
            comment = "",
            tags = emptyList(),
            createdAt = System.currentTimeMillis(),

            // ✅ Новые поля
            name = "",
            country = "",
            store = "",
            price = 0f
        )

        firestore.collection("Folders").document(folder).collection("Photos")
            .document(publicId)
            .set(photo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    fun updatePhoto(
        folder: String,
        photoId: String,
        comment: String,
        tags: List<String>,

        name: String,
        country: String,
        store: String,
        price: Float,

        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "comment" to comment,
            "tags" to tags,
            "country" to country,
            "store" to store,
            "price" to price,
        )

//        if (tags.isEmpty()) {
//            updates["tags"] = FieldValue.delete()
//        } else {
//            updates["tags"] = tags
//        }

        firestore.collection("Folders").document(folder).collection("Photos")
            .document(photoId)
            .update(updates)
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

        val publicIds = photoUrls.map { url ->
            "PRODUCTUS/" + url.substringAfterLast("/").substringBeforeLast(".") // ✅ Добавляем путь
        }

        Log.d("PhotoRepository", "Удаляем фото из Cloudinary: $publicIds") // ✅ Логируем удаляемые файлы

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deleteResults = publicIds.map { publicId ->
                    MediaManager.get().cloudinary.uploader().destroy(publicId, null)
                }

                val successfulDeletes = deleteResults.count { it?.get("result") == "ok" }
                Log.d("PhotoRepository", "Успешно удалено фото: $successfulDeletes из ${publicIds.size}")

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Ошибка массового удаления из Cloudinary: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }


    fun deletePhoto(folder: String, photoId: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("Folders").document(folder).collection("Photos")
            .document(photoId)
            .delete()
            .addOnSuccessListener {
                // В пути должно быть PRODUCTUS
//                val parts = imageUrl.split("/upload/")[1].split("/")
//                val publicId = parts.drop(1).joinToString("/").substringBeforeLast(".")
                val publicId = "PRODUCTUS/" + imageUrl.substringAfterLast("/").substringBeforeLast(".")


                Log.d("PhotoRepository", "Extracted publicId: $publicId from URL: $imageUrl")

                if (publicId.isEmpty()) {
                    Log.e("PhotoRepository", "Invalid publicId extracted from URL: $imageUrl")
                    onFailure(Exception("Invalid publicId"))
                    return@addOnSuccessListener
                }

                // Запускаем удаление из Cloudinary в фоновом потоке
                CoroutineScope(Dispatchers.IO).launch {
                    try {

                        val deleteResult = MediaManager.get().cloudinary.uploader().destroy(publicId, null)
                        if (deleteResult != null && deleteResult.get("result") == "ok") {
                            Log.d("PhotoRepository", "Фото удалено из Cloudinary: $publicId, result: $deleteResult")
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else {
                            Log.e("PhotoRepository", "Cloudinary deletion failed: $deleteResult")
                            withContext(Dispatchers.Main) {
                                onFailure(Exception("Cloudinary deletion failed"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PhotoRepository", "Ошибка удаления из Cloudinary: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            onFailure(e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PhotoRepository", "Ошибка удаления из Firebase", e)
                onFailure(e)
            }
    }





}
