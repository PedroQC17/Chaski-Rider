package com.example.chaskirider.core.platform

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileProviderUris {
    fun authority(context: Context): String = "${context.packageName}.files"

    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    fun belongsTo(context: Context, uri: Uri): Boolean =
        uri.scheme == "content" && uri.authority == authority(context)
}
