package com.example.sensus.data

import com.example.sensus.data.remote.buildLoggingInterceptor
import com.example.sensus.data.remote.httpLoggingLevel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FT-27 (RF-27-03): el logging HTTP no debe filtrar tokens JWT, contraseñas ni PII. En `release` no registra nada
 * y en `debug` solo registra método, URL y código.
 */
class NetworkLoggingSecurityTest {

    private val server = MockWebServer()
    private val logged = mutableListOf<String>()

    @Before
    fun setup() {
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun send(isDebug: Boolean) {
        val client = OkHttpClient.Builder()
            .addInterceptor(buildLoggingInterceptor(isDebug) { logged += it })
            .build()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"token":"respuesta-secreta"}"""))
        val body = """{"email":"a@b.com","password":"Contrasena-Secreta-1"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(server.url("/api/v1/auth/login"))
            .header("Authorization", "Bearer jwt-super-secreto")
            .post(body)
            .build()
        client.newCall(request).execute().close()
    }

    @Test
    fun releaseBuild_logsNothingAtAll() {
        send(isDebug = false)

        assertTrue("En release no debe registrarse ninguna línea: $logged", logged.isEmpty())
    }

    @Test
    fun debugBuild_neverLogsTokensPasswordsOrBodies() {
        send(isDebug = true)

        val all = logged.joinToString("\n")
        assertTrue("Debug debe registrar al menos método y URL", all.contains("POST"))
        assertFalse(all.contains("jwt-super-secreto"))
        assertFalse(all.contains("Contrasena-Secreta-1"))
        assertFalse(all.contains("respuesta-secreta"))
        assertFalse(all.contains("a@b.com"))
    }

    @Test
    fun loggingLevel_isNoneInReleaseAndBasicInDebug() {
        assertEquals(HttpLoggingInterceptor.Level.NONE, httpLoggingLevel(isDebug = false))
        assertEquals(HttpLoggingInterceptor.Level.BASIC, httpLoggingLevel(isDebug = true))
    }

    @Test
    fun interceptor_neverUsesBodyOrHeadersLevel() {
        listOf(true, false).forEach { isDebug ->
            val level = buildLoggingInterceptor(isDebug).level
            assertFalse(level == HttpLoggingInterceptor.Level.BODY || level == HttpLoggingInterceptor.Level.HEADERS)
        }
    }
}
