package integration.traits

import academy.kt.fromJson
import adapters.rest.DayStatisticsJson
import adapters.rest.StatisticsJson
import io.ktor.http.*
import io.ktor.server.testing.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface PageLoadsTrait {

    fun TestApplicationEngine.getStats(
        userUuid: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): StatisticsJson? {
        with(handleRequest(HttpMethod.Get, "/statistics") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader("userUuid", userUuid)
        }) {
            assertTrue(requestHandled)
            assertEquals(statusCode, response.status())
            return response.content?.fromJson()
        }
    }

    fun TestApplicationEngine.getPageStats(
        userUuid: String,
        pageKey: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): StatisticsJson? {
        with(handleRequest(HttpMethod.Get, "/statistics/$pageKey") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader("userUuid", userUuid)
        }) {
            assertTrue(requestHandled)
            assertEquals(statusCode, response.status())
            return response.content?.fromJson()
        }
    }

    fun TestApplicationEngine.getDayStats(
        userUuid: String,
        day: LocalDate,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): DayStatisticsJson? {
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
        with(handleRequest(HttpMethod.Get, "/statistics/day/${day.toString(formatter)}") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader("userUuid", userUuid)
        }) {
            assertTrue(requestHandled)
            assertEquals(statusCode, response.status())
            return response.content?.fromJson()
        }
    }

    fun TestApplicationEngine.getArticlesStats(
        userUuid: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): StatisticsJson? {
        with(handleRequest(HttpMethod.Get, "/statistics/articles") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader("userUuid", userUuid)
        }) {
            assertTrue(requestHandled)
            assertEquals(statusCode, response.status())
            return response.content?.fromJson()
        }
    }

    fun TestApplicationEngine.postPageLoad(
        userUuid: String,
        pageKey: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ) {
        with(handleRequest(HttpMethod.Post, "/page-load") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader("userUuid", userUuid)
            setBody(
                """{
                        "pageKey": "$pageKey"
                    }""".trimIndent()
            )
        }) {
            assertTrue(requestHandled)
            assertEquals(statusCode, response.status())
        }
    }
}
