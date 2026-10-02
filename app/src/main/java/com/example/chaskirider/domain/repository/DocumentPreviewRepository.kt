package com.example.chaskirider.domain.repository
import android.net.Uri
import com.example.chaskirider.domain.model.DocumentPage
interface DocumentPreviewRepository {
    suspend fun render(uri: Uri, page: Int): Result<DocumentPage>
}
