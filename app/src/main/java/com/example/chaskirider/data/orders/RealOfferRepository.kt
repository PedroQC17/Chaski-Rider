package com.example.chaskirider.data.orders

import android.net.Uri
import com.example.chaskirider.domain.orders.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repositorio real que se conecta al backend de Firebase Cloud Functions.
 * Usa la API real del backend para procesar pedidos.
 */
class RealOfferRepository : OfferRepository {

    //variable que conecta con la api con firebase con el servidor de la nube
    private val api = Retrofit.Builder()
        .baseUrl("https://us-central1-chaski-rider.cloudfunctions.net/")
        .client(OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(OfferApi::class.java)

    override suspend fun execute(
        action: OfferAction,
        offerId: String?,
        lat: Double?,
        lng: Double?,
        evidenceUrl: String?,
        deliveryNote: String?,
    ): OfferSnapshot {
        val user = FirebaseAuth.getInstance().currentUser ?: throw OfferRequestException(401)
        val token = user.getIdToken(false).await().token ?: throw OfferRequestException(401)

        val finalEvidenceUrl = if (action == OfferAction.DELIVER) {
            uploadEvidencePhoto(user.uid, offerId, evidenceUrl)
        } else evidenceUrl

        try {
            return api.execute("Bearer $token", OfferRequest(
                action = action.name.lowercase(Locale.ROOT),
                offerId = offerId,
                requestId = UUID.randomUUID().toString(),
                latitude = lat,
                longitude = lng,
                evidencePhotoUrl = finalEvidenceUrl,
                deliveryNote = deliveryNote,
            )).toDomain()
        } catch (e: HttpException) { throw OfferRequestException(e.code()) }
    }

    private suspend fun uploadEvidencePhoto(uid: String, offerId: String?, evidenceUrl: String?): String? {
        if (evidenceUrl.isNullOrBlank() || (!evidenceUrl.startsWith("content://") && !evidenceUrl.startsWith("file://"))) {
            return evidenceUrl
        }
        return try {
            val uri = Uri.parse(evidenceUrl)
            val path = "riders/$uid/orders/${offerId ?: "order_${System.currentTimeMillis()}"}/evidence_${System.currentTimeMillis()}.jpg"
            val ref = FirebaseStorage.getInstance().reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            evidenceUrl
        }
    }
}
