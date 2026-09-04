package com.indian.nutrition.tracker.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffApiClientTest {

    private val validProducts = """
        {"products":[
          {"code":"8901","product_name":"Amul Butter","brands":"Amul",
           "nutriments":{"energy-kcal_100g":740,"proteins_100g":0.6,
                         "carbohydrates_100g":0.4,"fat_100g":82}}
        ]}
    """.trimIndent()

    private fun productsResponse(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setBody(body)

    @Test
    fun usesInApiFirst() = runTest {
        val server = MockWebServer()
        server.enqueue(productsResponse(validProducts))
        server.start()
        try {
            val client = OffApiClient(
                inBaseUrl = server.url("/").toString(),
                worldBaseUrl = server.url("/").toString(),
            )
            val foods = client.search("butter")
            assertEquals(1, foods.size)
            assertEquals("Amul Butter", foods[0].name)

            val request = server.takeRequest()
            assertEquals("cgi/search.pl", request.path?.substringBefore("?")?.trimStart('/'))
            assertTrue(request.path!!.contains("search_terms=butter"))
            // Real OFF User-Agent with a genuine contact — web sent a fake one.
            assertTrue(request.getHeader("User-Agent")!!.contains("torwer2021@gmail.com"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun fallsBackToWorldWhenInEmpty() = runTest {
        val inServer = MockWebServer()
        val worldServer = MockWebServer()
        inServer.enqueue(productsResponse("""{"products":[]}"""))
        worldServer.enqueue(productsResponse(validProducts))
        inServer.start(); worldServer.start()
        try {
            val client = OffApiClient(
                inBaseUrl = inServer.url("/").toString(),
                worldBaseUrl = worldServer.url("/").toString(),
            )
            val foods = client.search("butter")
            assertEquals(1, foods.size)
            assertEquals("Amul Butter", foods[0].name)
            assertEquals(1, inServer.requestCount)
            assertEquals(1, worldServer.requestCount)
        } finally {
            inServer.shutdown(); worldServer.shutdown()
        }
    }

    @Test
    fun fallsBackToWorldWhenInFails() = runTest {
        val inServer = MockWebServer()
        val worldServer = MockWebServer()
        inServer.enqueue(productsResponse("""{"error":"rate limited"}""", 429))
        worldServer.enqueue(productsResponse(validProducts))
        inServer.start(); worldServer.start()
        try {
            val client = OffApiClient(
                inBaseUrl = inServer.url("/").toString(),
                worldBaseUrl = worldServer.url("/").toString(),
            )
            val foods = client.search("butter")
            assertEquals(1, foods.size)
        } finally {
            inServer.shutdown(); worldServer.shutdown()
        }
    }

    @Test
    fun throwsWhenBothHostsFail() = runTest {
        val inServer = MockWebServer()
        val worldServer = MockWebServer()
        inServer.enqueue(productsResponse("""{"error":"x"}""", 500))
        worldServer.enqueue(productsResponse("""{"error":"x"}""", 500))
        inServer.start(); worldServer.start()
        try {
            val client = OffApiClient(
                inBaseUrl = inServer.url("/").toString(),
                worldBaseUrl = worldServer.url("/").toString(),
            )
            val thrown = try {
                client.search("butter")
                null
            } catch (e: Exception) {
                e
            }
            assertTrue(thrown != null)
        } finally {
            inServer.shutdown(); worldServer.shutdown()
        }
    }
}
