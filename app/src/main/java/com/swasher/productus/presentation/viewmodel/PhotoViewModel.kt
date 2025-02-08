package com.swasher.productus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasher.productus.data.model.Photo
import com.swasher.productus.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class PhotoViewModel : ViewModel() {
    private val repository = PhotoRepository()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    fun loadPhotos() {
        repository.getPhotos(
            onSuccess = { _photos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    init {
        observePhotos()
    }

    private fun observePhotos() {
        repository.observePhotos(
            onUpdate = { _photos.value = it },
            onFailure = { it.printStackTrace() }
        )
    }

    fun updatePhoto(photoId: String, comment: String, tags: List<String>) {
        repository.updatePhoto(
            photoId, comment, tags,
            onSuccess = { loadPhotos() }, // Перезагружаем список фото после обновления
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

}
