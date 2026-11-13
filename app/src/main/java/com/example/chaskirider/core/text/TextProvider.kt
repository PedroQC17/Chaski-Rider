package com.example.chaskirider.core.text

interface TextProvider {
    fun get(key: TextKey): String
    fun resolveError(message: String?, fallback: TextKey): String
}
