package com.example.chaskirider.feature.documents.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Upsert
import com.example.chaskirider.feature.documents.domain.DocumentPolicy
import com.example.chaskirider.feature.documents.domain.DocumentReviewStatus
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot

@Entity(tableName = "documents", primaryKeys = ["riderId", "type"])
data class DocumentEntity(
    val riderId: String,
    val type: String,
    val storagePath: String,
    val status: String,
    val reviewReason: String,
    val updatedAt: Long
)

@Dao
interface DocumentDao {
    @Upsert
    suspend fun saveAll(entities: List<DocumentEntity>)
}

class DocumentStore(private val dao: DocumentDao) {
    suspend fun save(riderId: String, snapshot: DocumentSnapshot) {
        val now = System.currentTimeMillis()
        val entities = DocumentPolicy.types.map { type ->
            val review = snapshot.reviews[type]
            DocumentEntity(
                riderId = riderId,
                type = type,
                storagePath = snapshot.paths[type].orEmpty(),
                status = (review?.status ?: DocumentReviewStatus.PENDING).name,
                reviewReason = review?.reason.orEmpty(),
                updatedAt = now
            )
        }
        dao.saveAll(entities)
    }
}
