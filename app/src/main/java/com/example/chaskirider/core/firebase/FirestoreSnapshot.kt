package com.example.chaskirider.core.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

suspend fun DocumentReference.getWithCacheFallback(online: Boolean = true): DocumentSnapshot {
    if (!online) return get(Source.CACHE).await()
    return try {
        withTimeout(8_000) { get(Source.SERVER).await() }
    } catch (_: TimeoutCancellationException) {
        get(Source.CACHE).await()
    } catch (error: FirebaseFirestoreException) {
        if (error.code !in listOf(FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)) throw error
        get(Source.CACHE).await()
    }
}
