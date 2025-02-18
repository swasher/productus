/*
Отвечает за работу с данными. Работает с Firebase и Cloudinary. Он не знает про UI.

✅ Достаёт данные из Firestore
✅ Загружает фото в Cloudinary
✅ Обновляет Firestore
✅ Удаляет фото из Cloudinary
*/

package com.swasher.productus.data.repository

import android.util.Log
import com.cloudinary.android.MediaManager

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.swasher.productus.data.model.Photo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.swasher.productus.BuildConfig
import java.io.File
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo
import javax.inject.Inject
import javax.inject.Singleton


fun getThumbnailUrl(imageUrl: String, width: Int = 200, height: Int = 200): String {
    // c_auto - автоматески подгоняет под размер
    // g_auto - gravity, в центрирует по сюжету
    // return imageUrl.replace("/upload/", "/upload/w_${width},h_${height},c_auto,g_auto/")
    return imageUrl.replace("/upload/", "/upload/w_${width},h_${height},c_fit/")
}


@Singleton
class PhotoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
)  {

    private val userId: String
        get() = authRepository.getCurrentUser()?.uid
            ?: throw IllegalStateException("User must be authenticated to perform this operation")

    private val userFolder: String
        get() = "User-$userId"

    // Теперь отображает папки залогиненного юзера
    fun getFolders(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {

        firestore.collection(userFolder)
            .get()
            .addOnSuccessListener { snapshot ->
                val folders = snapshot.documents.map { it.id }
                onSuccess(folders)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Получаем фото внутри конкретной папки
    fun getPhotos(folder: String, onSuccess: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        Log.d("PhotoRepository", "Fetching photos for folder: $folder")
        // val uid = userId ?: return

        firestore.collection(userFolder).document(folder).collection("Photos")
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

    // Загружаем всю коллекцию локально для полнотекстового поиска
    fun getAllPhotos(onSuccess: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId ?: return

        firestore.collectionGroup("Photos")
            .get()
            .addOnSuccessListener { snapshot ->
                val photos = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
                onSuccess(photos)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Сохраняем фото в конкретную коллекцию (папку)
    // СОХРАНЕНИЕ используется для новой фотки! (для обновления есть updatePhoto)
    fun saveDataToFirebase(
        folder: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val publicId = imageUrl.substringAfterLast("/").substringBeforeLast(".") // Извлекаем `vivzby7juh6ph5g4nywq.jpg`, Убираем расширение `.jpg`

        val photo = Photo(
            id = publicId,
            imageUrl = imageUrl,
            folder = userFolder,
            comment = "",
            tags = emptyList(),
            createdAt = System.currentTimeMillis(),

            // ✅ Новые поля
            name = "",
            country = "",
            store = "",
            price = 0f
        )

        firestore.collection(userFolder).document(folder).collection("Photos")
            .document(publicId)
            .set(photo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }



    fun createFolder(folderName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId ?: return // ✅ Если пользователя нет, ничего не делаем

        firestore.collection(userFolder) // ✅ Теперь создаем коллекцию внутри "User-<userId>"
            .document(folderName)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    fun renameFolder(oldName: String, newName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId ?: return // ✅ Если пользователя нет, ничего не делаем

        val oldFolderRef = firestore.collection(userFolder).document(oldName)
        val newFolderRef = firestore.collection(userFolder).document(newName)

        Log.d("PhotoRepository", "Переименование папки: $oldName в $newName")

        // ✅ Создаём новую папку (метаданные)
        newFolderRef.set(mapOf("createdAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                oldFolderRef.collection("Photos").get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()
                        Log.d("PhotoRepository", "Snapshot: $snapshot")

                        snapshot.documents.forEach { doc ->
                            val newDocRef = newFolderRef.collection("Photos").document(doc.id)
                            batch.set(newDocRef, doc.data ?: emptyMap<String, Any>()) // ✅ Копируем фото в новую папку
                            batch.delete(doc.reference) // ✅ Удаляем из старой папки
                        }

                        batch.commit().addOnSuccessListener {
                            oldFolderRef.delete() // ✅ Удаляем старую папку
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { onFailure(it) }
                        }.addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
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
        rating: Int,

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
            "rating" to rating,
        )

        firestore.collection(userFolder).document(folder).collection("Photos")
            .document(photoId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    fun observeFolders(onUpdate: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection(userFolder)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                val folders = snapshot?.documents?.map { it.id } ?: emptyList()
                onUpdate(folders)
            }
    }

    fun observePhotos(folder: String, onUpdate: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        // Наблюдение изменений фото в конкретной папке
        firestore.collection(userFolder).document(folder).collection("Photos")
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

    fun observeAllPhotos(onUpdate: (List<Photo>) -> Unit, onFailure: (Exception) -> Unit) {
        // Наблюдение изменений ВСЕХ фото юзера для ПОИСКА
        firestore.collectionGroup("Photos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val photos = snapshot?.documents?.mapNotNull {
                    it.toObject(Photo::class.java)
                } ?: emptyList()
                onUpdate(photos)
            }
    }

    private fun deletePhotosFromCloudinary(photoUrls: List<String>, onComplete: () -> Unit) {
        if (photoUrls.isEmpty()) {
            onComplete() // Если фото нет, просто завершаем
            return
        }

        val publicIds = photoUrls.map { url ->
            BuildConfig.CLOUDINARY_UPLOAD_DIR + "/" + url.substringAfterLast("/").substringBeforeLast(".") // ✅ Добавляем путь
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
        firestore.collection(userFolder).document(folder).collection("Photos")
            .document(photoId)
            .delete()
            .addOnSuccessListener {
                val publicId = BuildConfig.CLOUDINARY_UPLOAD_DIR + "/" + imageUrl.substringAfterLast("/").substringBeforeLast(".")


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

    fun deleteFolder(folder: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId ?: return // ✅ Если пользователь не залогинен, ничего не делаем

        val folderRef = firestore.collection(userFolder).document(folder).collection("Photos")

        folderRef.get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            val photoUrls = mutableListOf<String>()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
                val photo = doc.toObject(Photo::class.java)
                photo?.imageUrl?.let { photoUrls.add(it) } // ✅ Собираем ссылки на фото
            }

            batch.commit().addOnSuccessListener {
                deletePhotosFromCloudinary(photoUrls) {
                    firestore.collection(userFolder).document(folder).delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }

    fun uploadPhotoToCloudinary(photoPath: String, folder: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val file = File(photoPath).absolutePath
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onFailure(Exception("User not authenticated"))
        val cloudinaryUploadDir = BuildConfig.CLOUDINARY_UPLOAD_DIR
        val cloudinaryFolder = "${cloudinaryUploadDir}/USER_${userId}"

        val request = MediaManager.get().upload(file)
            .option("folder", cloudinaryFolder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    onSuccess(imageUrl) // ✅ Передаем URL в Firestore
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onFailure(Exception(error.description))
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }


    fun loadFolderCounts(onUpdate: (Map<String, Int>) -> Unit, onFailure: (Exception) -> Unit) {
        val counts = mutableMapOf<String, Int>()

        firestore.collection(userFolder)
            .get()
            .addOnSuccessListener { snapshot ->
                val folders = snapshot.documents.map { it.id }
                var completedFolders = 0

                folders.forEach { folder ->
                    Log.d("PhotoRepository", "Чтение папки: $folder")
                    firestore.collection(userFolder)
                        .document(folder)
                        .collection("Photos")
                        .get()
                        .addOnSuccessListener { photosSnapshot ->
                            counts[folder] = photosSnapshot.size()
                            Log.d("PhotoRepository", "Папка: $folder, Количество фото: ${photosSnapshot.size()}")
                            completedFolders++

                            if (completedFolders == folders.size) {
                                onUpdate(counts)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("PhotoRepository", "Ошибка чтения фото в папке: $folder", it)
                            onFailure(it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("PhotoRepository", "Ошибка чтения папок", it) // Логируем ошибку
                onFailure(it)
            }
    }

}
