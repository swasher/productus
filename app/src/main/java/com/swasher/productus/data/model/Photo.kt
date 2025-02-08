package com.swasher.productus.data.model

data class Photo(
    val id: String = "",
    val imageUrl: String = "",
    val comment: String = "",
    val tags: List<String> = emptyList(),
    val folder: String = "Unsorted",
    val createdAt: Long = System.currentTimeMillis()
)
