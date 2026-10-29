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

        //user es el usuario actual de firebase y token es el token de acceso
        val user = FirebaseAuth.getInstance().currentUser ?: throw OfferRequestException(401)
        val token = user.getIdToken(false).await().token ?: throw OfferRequestException(401)

        var finalEvidenceUrl = evidenceUrl
        if (action == OfferAction.DELIVER && evidenceUrl != null && (evidenceUrl.startsWith("content://") || evidenceUrl.startsWith("file://"))) {
            try {
                val uri = Uri.parse(evidenceUrl)
                val path = "riders/${user.uid}/orders/${offerId ?: "order_${System.currentTimeMillis()}"}/evidence_${System.currentTimeMillis()}.jpg"
                val fileRef = FirebaseStorage.getInstance().reference.child(path)
                fileRef.putFile(uri).await()
                finalEvidenceUrl = fileRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            // se usa el token para autenticar la peticion y se hace la peticion a la api
            return api.execute("Bearer $token", OfferRequest(
                action= action.name.lowercase(Locale.ROOT),
                offerId= offerId,
                requestId=UUID.randomUUID().toString(),
                latitude=lat,
                longitude =lng,
                //campos para validar la finalizacion de un pedido
                evidencePhotoUrl = finalEvidenceUrl,
                deliveryNote = deliveryNote,
            )).toDomain()
        } catch (e: HttpException) { throw OfferRequestException(e.code()) }
    }
}
