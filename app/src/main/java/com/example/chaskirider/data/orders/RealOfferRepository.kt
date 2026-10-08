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
    private val api = Retrofit.Builder()
        .baseUrl("https://us-central1-chaski-rider.cloudfunctions.net/")
        .client(OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(OfferApi::class.java)

    override suspend fun execute(
        action: OfferAction,
        offerId: String?,
        lat: Double?,
        lng: Double?
    ): OfferSnapshot {
        val user = FirebaseAuth.getInstance().currentUser ?: throw OfferRequestException(401)
        val token = user.getIdToken(false).await().token ?: throw OfferRequestException(401)
        try {
            return api.execute("Bearer $token", OfferRequest(action.name.lowercase(Locale.ROOT), offerId,
                UUID.randomUUID().toString(), lat, lng)).toDomain()
        } catch (e: HttpException) { throw OfferRequestException(e.code()) }
    }
}
