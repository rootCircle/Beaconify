// network/BeaconApiService.kt
package com.iiitl.locateme.network

import com.iiitl.locateme.network.models.BeaconResponse
import com.iiitl.locateme.network.models.DeactivateBeaconRequest
import com.iiitl.locateme.network.models.VirtualBeacon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface BeaconApi {
    @GET("api/getAllVBeacons")
    suspend fun getAllBeacons(): BeaconResponse

    @POST("api/pollVBeacons")
    suspend fun registerBeacon(@Body beacon: VirtualBeacon): BeaconResponse

    @POST("api/deactivateVBeacon")
    suspend fun deactivateBeacon(@Body beacon: DeactivateBeaconRequest): BeaconResponse
}

object BeaconApiService {
    private const val BASE_URL = "https://beaconify.vercel.app/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val beaconApi = retrofit.create(BeaconApi::class.java)

    suspend fun getAllBeacons(): Result<List<VirtualBeacon>> = withContext(Dispatchers.IO) {
        try {
            val response = beaconApi.getAllBeacons()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerBeacon(beacon: VirtualBeacon): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = beaconApi.registerBeacon(beacon)
            if (response.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateBeacon(uuid: String, major: String, minor: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = DeactivateBeaconRequest(uuid, major, minor)
                val response = beaconApi.deactivateBeacon(request)
                if (response.success) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.message ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}