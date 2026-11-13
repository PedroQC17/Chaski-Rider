package com.example.chaskirider.feature.documents.data

import android.net.Uri
import com.example.chaskirider.feature.documents.data.local.RiderDocumentDataSource
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import com.example.chaskirider.core.platform.FileProviderUris
import com.example.chaskirider.feature.documents.domain.DocumentPage
import com.example.chaskirider.feature.documents.domain.DocumentPreviewRepository
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.core.firebase.firebaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepositoryImpl(private val documents: RiderDocumentDataSource) : DocumentRepository {
    override suspend fun load() = documents.load()
    override suspend fun createCapture() = documents.createCapture()
    override suspend fun hasCapture(uri: Uri) = documents.hasCapture(uri)
    override suspend fun deleteCapture(uri: Uri) = documents.deleteCapture(uri)
    override suspend fun uploadDocument(docType: String, uri: Uri) = documents.uploadDocument(docType, uri)
    override suspend fun getDocument(docType: String) = documents.getDocument(docType)
}

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
                        DocumentPage(FileProviderUris.uriFor(context, file), page, renderer.pageCount)
                    } finally { bitmap.recycle() }
                }
            }
        }
    }
}
