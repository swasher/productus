/*
Управляет UI-логикой. Знает про UI, но не знает, как устроена Firestore или Cloudinary.

Что делает PhotoViewModel:
✅ Управляет UI-данными
✅ Обрабатывает события пользователя (например, обновление фото)
✅ Вызывает PhotoRepository, но не знает, как тот работает
*/

package com.swasher.productus.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class PhotoViewModel : ViewModel() {
    private val repository = PhotoRepository()
    // private val firestore = FirebaseFirestore.getInstance()
    private val userId: String?get() = FirebaseAuth.getInstance().currentUser?.uid
    private val userFolder = "User-$userId"

    // для отбора фото внутри папки
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    // для фильтрации по тегам внутри папки
    private val _filterTag = MutableStateFlow<String?>(null)
    val filterTag = _filterTag.asStateFlow()

    // непонятно для чего, возможно не используется
    private val _filterFolder = MutableStateFlow<String?>(null)
    val filterFolder = _filterFolder.asStateFlow()

    // для обновления списка папок после добавления или удаления папки
    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    // поиск по предварительно скачанной коллекции Firebase
    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList()) // ✅ Все фото в памяти
    val allPhotos: StateFlow<List<Photo>> = _allPhotos.asStateFlow()

    // для глобального поиска
    private val _searchResults = MutableStateFlow<List<Photo>>(emptyList())
    val searchResults: StateFlow<List<Photo>> = _searchResults.asStateFlow()

    // для процесса аплоада
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        //loadAllPhotos() // ✅ Загружаем коллекцию при запуске
        observeFolders()
    }


    // TODO startObservingPhotos и observePhotos наверное надо объеденить(спросить МОСК)

    private fun observeFolders() {
        repository.observeFolders(
            onUpdate = { _folders.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun observePhotos(folder: String) {
        repository.observePhotos(
            folder = folder,
            onUpdate = { _photos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }
    // fun startObservingPhotos(folder: String) {
    //     observePhotos(folder)
    // }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PhotoViewModel()
            }
        }
    }

    // possible deprecated
    // может понадобиться в будущем, если нужен будет метод для загрузки всех фото
    private fun loadAllPhotos() {
        repository.getAllPhotos(
            onSuccess = { _allPhotos.value = it },
            onFailure = { it.printStackTrace() }
        )
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

    fun loadFolders() {
        repository.getFolders(
            onSuccess = { _folders.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun createFolder(folderName: String) {
        repository.createFolder(
            folderName = folderName,
            onSuccess = { loadFolders() }, // ✅ Перезагружаем список папок
            onFailure = { it.printStackTrace() }
        )
    }


    fun renameFolder(oldName: String, newName: String) {
        repository.renameFolder(
            oldName = oldName,
            newName = newName,
            onSuccess = { loadFolders() }, // ✅ Перезагружаем список папок
            onFailure = { it.printStackTrace() }
        )
    }


    fun deleteFolder(folder: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        repository.deleteFolder(
            folder = folder,
            onSuccess = {
                loadFolders() // ✅ Перезагружаем список папок
                onSuccess()
            },
            onFailure = { onFailure(it) }
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


    // PREVIOUS VERIOSN
    // fun updatePhoto(folder: String, photoId: String, comment: String, tags: List<String>, name: String, country: String, store: String, price: Float) {
    //     val cleanedTags = tags.map { it.trim() }.filter { it.isNotBlank() } // ✅ Убираем пустые строки
    //
    //     repository.updatePhoto(
    //         folder = folder, // 📌 Передаём имя коллекции
    //         photoId = photoId,
    //         comment = comment,
    //         tags = cleanedTags,
    //         name = name,
    //         country = country,
    //         store = store,
    //         price = price,
    //         onSuccess = { loadPhotos(folder) }, // 📌 Загружаем фото только из нужной папки
    //         onFailure = { it.printStackTrace() }
    //     )
    // }


    fun updatePhoto(folder: String, photoId: String, comment: String, tags: List<String>, name: String, country: String, store: String, price: Float) {
        val cleanedTags = tags.map { it.trim() }.filter { it.isNotBlank() } // ✅ Убираем пустые строки

        repository.updatePhoto(
            folder = folder,
            photoId = photoId,
            comment = comment,
            tags = cleanedTags,
            name = name,
            country = country,
            store = store,
            price = price,
            onSuccess = {
                // 🔥 Вместо загрузки всех фото просто обновляем локальное состояние
                _photos.value = _photos.value.map { photo ->
                    if (photo.id == photoId) {
                        photo.copy(
                            comment = comment,
                            tags = cleanedTags,
                            name = name,
                            country = country,
                            store = store,
                            price = price
                        )
                    } else photo
                }
            },
            onFailure = { it.printStackTrace() }
        )
    }


    // список фото для вывода на экране "Список фото в папке"
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

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    // Вариант поиска с возвратом Flow (через запрос к Firestore)
    /*
    suspend fun searchPhotos(query: String): Flow<List<Photo>> = flow {
        if (query.isBlank()) {
            emit(emptyList<Photo>()) // Если строка пустая, очищаем список
            return@flow
        }

        firestore.collectionGroup("Photos") // 📌 Ищем во всех папках
            .orderBy("name") // 🔍 Улучшает сортировку (но требует индекса)
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { snapshot ->
                val photos = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
                emit(photos) // ✅ Отправляем список найденных фото
            }
            .addOnFailureListener { emit(emptyList<Photo>()) }
    }.debounce(300) // ⏳ Добавляем задержку 300ms для оптимизации
    */


    // Вариант поиска через запрос к Firestore
    /*
    // TODO добавить .debounce(300)
    suspend fun searchPhotos(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList<Photo>() // ✅ Если строка пустая, очищаем список
            return
        }

        Log.d("PhotoViewModel", "Поиск: $query")

        try {
            val snapshot: QuerySnapshot = withContext(Dispatchers.IO) {
                firestore.collectionGroup("Photos")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .get()
                    .await()
            }
            _searchResults.value = snapshot.documents.mapNotNull { it.toObject(Photo::class.java) }
            Log.d("PhotoViewModel", "Найдено фото: ${_searchResults.value.size}")
        } catch (e: Exception) {
            _searchResults.value = emptyList()
            Log.e("PhotoViewModel", "Ошибка поиска: ${e.message}")
        }
    }
    */

    // Вариант поиска через предзагруженную коллекцию Firestore
    fun searchPhotos(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList() // ✅ Если строка пустая, сбрасываем поиск
            return
        }

        val lowerQuery = query.lowercase()

        _searchResults.value = _allPhotos.value.filter { photo ->
            photo.name.lowercase().contains(lowerQuery) ||
            photo.comment.lowercase().contains(lowerQuery) ||
            photo.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }


    fun uploadPhoto(photoPath: String, folder: String) {
        _isUploading.value = true // ✅ Показываем индикатор загрузки

        repository.uploadPhotoToCloudinary(photoPath, folder,
            onSuccess = { imageUrl ->
                repository.saveDataToFirebase(
                    folder,
                    imageUrl,
                    onSuccess = { Log.d("CameraActivity", "✅ Фото сохранено в Firebase!")},
                    onFailure = {
                        it.printStackTrace()
                        Log.e("CameraActivity", "Ошибка сохранения фото в Firebase: ${it.message}")
                    }
                ) // ✅ Сохраняем в Firestore
                _isUploading.value = false // ✅ Скрываем индикатор загрузки
            },
            onFailure = {
                _isUploading.value = false
                it.printStackTrace()
            }
        )
    }




}
