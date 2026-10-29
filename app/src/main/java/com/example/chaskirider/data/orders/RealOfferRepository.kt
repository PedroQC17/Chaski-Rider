package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.*
import com.google.firebase.auth.FirebaseAuth
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
        //campos para validar la finalizacion de un pedido
        evidencePhotoUrl: String?,
        deliveryNote: String?,
    ): OfferSnapshot {

        //user es el usuario actual de firebase y token es el token de acceso
        val user = FirebaseAuth.getInstance().currentUser ?: throw OfferRequestException(401)
        val token = user.getIdToken(false).await().token ?: throw OfferRequestException(401)
        try {
            // se usa el token para autenticar la peticion y se hace la peticion a la api
            return api.execute("Bearer $token", OfferRequest(
                action= action.name.lowercase(Locale.ROOT),
                offerId= offerId,
                requestId=UUID.randomUUID().toString(),
                latitude=lat,
                longitude =lng,
                //campos para validar la finalizacion de un pedido
                evidencePhotoUrl = evidencePhotoUrl,
                deliveryNote = deliveryNote,
            )).toDomain()
        } catch (e: HttpException) { throw OfferRequestException(e.code()) }
    }
}
