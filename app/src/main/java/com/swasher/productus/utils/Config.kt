package com.swasher.productus.utils

import android.content.Context
import java.util.*

object Config {
    fun getProperty(context: Context, key: String): String? {
        val properties = Properties()
        val inputStream = context.assets.open("local.properties")
        properties.load(inputStream)
        return properties.getProperty(key)
    }
}
