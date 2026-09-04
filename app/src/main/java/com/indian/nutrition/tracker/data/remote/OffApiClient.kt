package com.indian.nutrition.tracker.data.remote

import com.indian.nutrition.tracker.domain.model.Food
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Open Food Facts search endpoint (same URL shape as the web app). */
interface OffApi {

    @GET("cgi/search.pl")
    suspend fun search(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: String,
        @Query("action") action: String,
        @Query("json") json: String,
        @Query("page_size") pageSize: Int,
        @Query("fields") fields: String,
    ): OffSearchResponseDto
}

/**
 * Open Food Facts client with the web fixes applied:
 * - real User-Agent with a genuine contact (OFF requirement; the web app
 *   sent a fake `support@nutritionapp.local`)
 * - direct IN API with WORLD fallback (no server proxy needed on device)
 * - 4-second connect/read/call timeouts (web used AbortController 4 s)
 * - products parsed leniently (numbers or strings) by [OffProductParser]
 */
class OffApiClient(
    inBaseUrl: String = "https://in.openfoodfacts.org/",
    worldBaseUrl: String = "https://world.openfoodfacts.org/",
    private val api: OffApi = buildApi(inBaseUrl),
    private val worldApi: OffApi = buildApi(worldBaseUrl),
) {

    /**
     * Search IN first, fall back to WORLD. Throws when both hosts fail or
     * return nothing usable — callers treat that as the offline path.
     */
    suspend fun search(query: String): List<Food> {
        var lastFailure: Exception? = null
        try {
            val foods = fetch(api, query)
            if (foods.isNotEmpty()) return foods
        } catch (e: Exception) {
            lastFailure = e
        }

        try {
            val foods = fetch(worldApi, query)
            if (foods.isNotEmpty()) return foods
        } catch (_: Exception) {
            // both hosts failed — fall through
        }
        throw lastFailure ?: NoOffResultsException(query)
    }

    private suspend fun fetch(api: OffApi, query: String): List<Food> {
        val response = api.search(
            searchTerms = query,
            searchSimple = "1",
            action = "process",
            json = "1",
            pageSize = 20,
            fields = "code,product_name,brands,nutriments,image_front_small_url,categories_tags",
        )
        return response.products.mapNotNull { OffProductParser.parseProduct(it) }
    }

    class NoOffResultsException(query: String) :
        Exception("Open Food Facts returned no results for '$query'")

    companion object {
        val USER_AGENT =
            "IndianNutritionTracker/1.0 (Android; contact: torwer2021@gmail.com)"

        /** 4-second timeouts, mirroring the web app's AbortController(4000). */
        fun buildClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .writeTimeout(4, TimeUnit.SECONDS)
                .callTimeout(4, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "application/json")
                            .build()
                    )
                }
                .build()

        private fun buildApi(baseUrl: String): OffApi {
            val json = Json { ignoreUnknownKeys = true }
            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(buildClient())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(OffApi::class.java)
        }
    }
}
