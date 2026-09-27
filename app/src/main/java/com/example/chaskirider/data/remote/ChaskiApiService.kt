package com.example.chaskirider.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ChaskiApiService {
    @GET("riders/{id}/profile")
    suspend fun getRiderProfile(@Path("id") id: String): Response<Map<String, Any>>
}
