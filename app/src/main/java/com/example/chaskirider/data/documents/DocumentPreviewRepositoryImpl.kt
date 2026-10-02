package com.example.chaskirider.data.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.chaskirider.domain.model.DocumentPage
import com.example.chaskirider.domain.repository.DocumentPreviewRepository
import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.data.remote.firebaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentPreviewRepositoryImpl(private val context: Context, private val texts: TextProvider) : DocumentPreviewRepository {
    override suspend fun render(uri: Uri, page: Int) = firebaseResult(texts) {
        withContext(Dispatchers.IO) {
            if (context.contentResolver.getType(uri) != "application/pdf") return@withContext DocumentPage(uri, 0, 1)
            val descriptor = requireNotNull(context.contentResolver.openFileDescriptor(uri, "r"))
            PdfRenderer(descriptor).use { renderer ->
                require(page in 0 until renderer.pageCount)
                renderer.openPage(page).use { pdf ->
                    val scale = minOf(1200f / pdf.width, 1800f / pdf.height)
                    val bitmap = Bitmap.createBitmap((pdf.width * scale).toInt().coerceAtLeast(1),
                        (pdf.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        pdf.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val directory = File(context.cacheDir, "documents").apply { mkdirs() }
                        val file = File(directory, "page-${uri.toString().hashCode()}-$page.png")
                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        DocumentPage(FileProvider.getUriForFile(context, "${context.packageName}.files", file), page, renderer.pageCount)
                    } finally { bitmap.recycle() }
                }
            }
        }
    }
}
