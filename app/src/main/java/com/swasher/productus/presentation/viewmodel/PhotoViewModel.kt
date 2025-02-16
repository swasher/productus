/*
Управляет UI-логикой. Знает про UI, но не знает, как устроена Firestore или Cloudinary.

Что делает PhotoViewModel:
✅ Управляет UI-данными
✅ Обрабатывает события пользователя (например, обновление фото)
✅ Вызывает PhotoRepository, но не знает, как тот работает
*/

package com.swasher.productus.presentation.viewmodel

import android.app.Application
import android.net.Uri
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
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class PhotoViewModel @Inject constructor(
    private val repository: PhotoRepository,
    private val application: Application // Добавляем инъекцию Application
) : ViewModel()  {
    // private val repository = PhotoRepository()
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

    // DEPRECATED
    // для процесса аплоада
    // private val _isUploading = MutableStateFlow(false)
    // val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _folderCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val folderCounts = _folderCounts.asStateFlow()

    // Состояние для хранения поискового запроса, чтобы при возврате на Search сохранялся список поиска
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        loadAllPhotos() // ✅ Загружаем коллекцию при запуске
        observeFolders()
        observeCollection() //для поиска фото
        Log.d("PhotoViewModel", "ViewModel initialized with hash: ${this.hashCode()}")
    }

    private fun observeFolders() {
        repository.observeFolders(
            onUpdate = { _folders.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun observePhotos(folder: String) {
        // наблюдение изменений фото в конкретной папке, например, обновление списка при добавлении фото
        repository.observePhotos(
            folder = folder,
            onUpdate = { _photos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    private fun observeCollection() {
        // наблюдение изменений фото в коллекции, например, для поиска при добавлении и или обновлении фото
        repository.observeAllPhotos(
            onUpdate = { _allPhotos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }


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
                // todo так как кеширование не работает, то это действие не имеет смысла, скорее всего УДАЛИТЬ
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
        }.sortedByDescending { it.createdAt }
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


    // Поиск через предзагруженную коллекцию Firestore
    fun searchPhotos(query: String) {
        Log.d("PhotoViewModel", "Поиск: $query")
        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList() // ✅ Если строка пустая, сбрасываем поиск
            return
        }

        val lowerQuery = query.lowercase()
        viewModelScope.launch {
            _searchResults.value = _allPhotos.value.filter { photo ->
                photo.name.lowercase().contains(lowerQuery) ||
                photo.comment.lowercase().contains(lowerQuery) ||
                photo.tags.any { it.lowercase().contains(lowerQuery) }
            }
            Log.d("PhotoViewModel", "Найдено фото: ${_searchResults.value.size}шт. среди ${_allPhotos.value.size}шт.")
        }
    }



    fun uploadPhoto(photoPath: String, folder: String) {
        Log.d("PhotoViewModel", "Starting upload, isUploading set to true")
        // _isUploading.value = true // ✅ Показываем индикатор

        // TODO так как кеширование не работает, то это действие не имеет смысла, скорее всего УДАЛИТЬ
        // 📌 Создаём временное фото с локальным путем (кеш)
        val tempPhoto = Photo(
            id = UUID.randomUUID().toString(),
            imageUrl = "file://$photoPath", // ✅ Временно используем кеш
            folder = folder,
            createdAt = System.currentTimeMillis(),
            isUploading = true  // Устанавливаем флаг загрузки
        )
        _photos.value = _photos.value + tempPhoto // ✅ Добавляем в UI

        repository.uploadPhotoToCloudinary(photoPath, folder,
            onSuccess = { imageUrl ->
                repository.saveDataToFirebase(folder, imageUrl,
                    onSuccess = {
                        Log.d("PhotoViewModel", "✅ Фото сохранено в Firebase!")

                        // ✅ Обновляем URL фото на Cloudinary-ссылку
                        _photos.value = _photos.value.map {
                            if (it.imageUrl == "file://$photoPath") it.copy(imageUrl = imageUrl) else it
                        }

                        // _isUploading.value = false // ✅ Скрываем индикатор
                        Log.d("PhotoViewModel", "Finish upload, isUploading set to false")
                    },
                    onFailure = {
                        it.printStackTrace()
                        Log.e("PhotoViewModel", "Ошибка сохранения в Firebase: ${it.message}")
                        // _isUploading.value = false  // Ошибка в Firebase
                    }
                )
            },
            onFailure = {
                // _isUploading.value = false // Ошибка в Cloudinary
                it.printStackTrace()
            }
        )
    }


    fun uploadPhotoFromUri(uri: Uri, folderName: String) {
        viewModelScope.launch {
            // _isUploading.value = true
            try {
                val file = createTempFileFromUri(uri)
                uploadPhoto(file.absolutePath, folderName)
            } catch (e: Exception) {
                Log.e("PhotoViewModel", "Error uploading local file", e)
            } finally {
                // _isUploading.value = false
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = application.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("temp_", ".jpg", application.cacheDir)

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    fun loadFolderCounts() {
        viewModelScope.launch {
            repository.loadFolderCounts(
                onUpdate = { counts -> _folderCounts.value = counts },
                onFailure = { it.printStackTrace() }
            )
        }
    }

}
