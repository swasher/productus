package com.swasher.productus.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.firestore.FirebaseFirestore


class PhotoViewModel : ViewModel() {
    private val repository = PhotoRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _filterTag = MutableStateFlow<String?>(null) // Текущий выбранный тег
    val filterTag = _filterTag.asStateFlow()

    private val _filterFolder = MutableStateFlow<String?>(null) // Текущая папка
    val filterFolder = _filterFolder.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders = _folders.asStateFlow()

    fun loadFolders() {
        repository.getFolders(
            onSuccess = { _folders.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun createFolder(folderName: String) {
        FirebaseFirestore.getInstance()
            .collection("Folders") // ✅ Теперь создаем коллекцию внутри "Folders"
            .document(folderName)
            .set(mapOf("createdAt" to System.currentTimeMillis())) // ✅ Добавляем метаданные, чтобы Firestore создал коллекцию
            .addOnSuccessListener { loadFolders() }
    }


//    fun renameFolder(oldName: String, newName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        val oldFolderRef = firestore.collection("Folders").document(oldName)
//        val newFolderRef = firestore.collection("Folders").document(newName)
//
//        oldFolderRef.collection("Photos").get()
//            .addOnSuccessListener { snapshot ->
//                val batch = firestore.batch()
//
//                snapshot.documents.forEach { doc ->
//                    val newDocRef = newFolderRef.collection("Photos").document(doc.id)
//                    // batch.set(newDocRef, doc.data ?: emptyMap()) // ✅ Копируем фото в новую папку
//                    // change by Claude:
//                    // batch.set(newDocRef, doc.data ?: emptyMap<String, Any>())
//
//                    batch.set(newDocRef, doc.data ?: emptyMap<String, Any>())
//                    batch.delete(doc.reference) // ✅ Удаляем из старой
//                }
//
//                batch.commit().addOnSuccessListener {
//                    oldFolderRef.delete() // ✅ Удаляем старую папку
//                        .addOnSuccessListener {
//                            loadFolders() // ✅ Обновляем список папок
//                            onSuccess()
//                        }
//                        .addOnFailureListener { onFailure(it) }
//                }.addOnFailureListener { onFailure(it) }
//            }
//            .addOnFailureListener { onFailure(it) }
//    }

    fun renameFolder(oldName: String, newName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val oldFolderRef = firestore.collection("Folders").document(oldName)
        val newFolderRef = firestore.collection("Folders").document(newName)

        // ✅ Создаём пустую новую папку (метаданные)
        newFolderRef.set(mapOf("createdAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                oldFolderRef.collection("Photos").get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()

                        snapshot.documents.forEach { doc ->
                            val newDocRef = newFolderRef.collection("Photos").document(doc.id)
                            batch.set(newDocRef, doc.data ?: emptyMap<String, Any>()) // ✅ Копируем фото в новую папку
                            batch.delete(doc.reference) // ✅ Удаляем из старой папки
                        }

                        batch.commit().addOnSuccessListener {
                            // ✅ Удаляем только метаданные старой папки, не трогая фото
                            oldFolderRef.delete()
                                .addOnSuccessListener {
                                    loadFolders() // ✅ Обновляем список папок
                                    onSuccess()
                                }
                                .addOnFailureListener { onFailure(it) }
                        }.addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }


    fun deleteFolder(folder: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val folderRef = firestore.collection("Folders").document(folder).collection("Photos")

        folderRef.get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            val photoUrls = mutableListOf<String>()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
                val photo = doc.toObject(Photo::class.java)
                photo?.imageUrl?.let { photoUrls.add(it) } // Собираем ссылки на фото
            }

            batch.commit().addOnSuccessListener {
                // 📌 Удаляем фото из Cloudinary
                repository.deletePhotosFromCloudinary(photoUrls) {
                    firestore.collection("Folders").document(folder).delete()
                        .addOnSuccessListener {
                            loadFolders()
                            onSuccess()
                        }
                        .addOnFailureListener { onFailure(it) }
                }
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }

    fun loadPhotos(folder: String) {
        Log.d("PhotoViewModel", "Загружаем фото для папки: $folder") // ✅ Логируем вызов
        repository.getPhotos(
            folder = folder,
            onSuccess = {
                _photos.value = it
                Log.d("PhotoViewModel", "Загружено фото: ${it.size}")
            },
            onFailure = {
                it.printStackTrace()
                Log.e("PhotoViewModel", "Ошибка загрузки фото: ${it.message}")
            }
        )
    }

    fun deletePhoto(folder: String, photoId: String, imageUrl: String) {
        repository.deletePhoto(
            folder = folder,
            photoId = photoId,
            imageUrl = imageUrl,
            onSuccess = { loadPhotos(folder) }, // ✅ Обновляем список фото
            onFailure = { it.printStackTrace() }
        )
    }


//    init {
//        observePhotos()
//    }

    fun startObservingPhotos(folder: String) {
        observePhotos(folder)
    }

    private fun observePhotos(folder: String) {
        repository.observePhotos(
            folder = folder,
            onUpdate = { _photos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun updatePhoto(folder: String, photoId: String, comment: String, tags: List<String>) {
        repository.updatePhoto(
            folder = folder, // 📌 Передаём имя коллекции
            photoId = photoId,
            comment = comment,
            tags = tags,
            onSuccess = { loadPhotos(folder) }, // 📌 Загружаем фото только из нужной папки
            onFailure = { it.printStackTrace() }
        )
    }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PhotoViewModel()
            }
        }
    }

    // Фильтрованный список фото
    val filteredPhotos = combine(photos, filterTag, filterFolder) { photos, tag, folder ->
        photos.filter {
            (tag == null || it.tags.contains(tag)) &&
                    (folder == null || it.folder == folder)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilterTag(tag: String?) {
        _filterTag.value = tag
    }

    fun setFilterFolder(folder: String?) {
        _filterFolder.value = folder
    }

}
