package domain.repository

import adapters.rest.DayStatisticsJson
import adapters.rest.StatisticsJson
import org.bson.codecs.pojo.annotations.BsonId
import org.joda.time.LocalDate
import java.time.Instant

interface PageStatsRepository {
    suspend fun addPageLoad(userUuid: String, pageKey: String)
    suspend fun getPageStatistics(pageKey: String): StatisticsJson
    suspend fun getDayStatistics(date: LocalDate): DayStatisticsJson
    suspend fun getPagesStatistics(): StatisticsJson
    suspend fun getArticlesStatistics(): StatisticsJson
}

data class PageLoad(
    @BsonId val _id: String,
    val userUuid: String,
    val timestamp: Instant,
    val pageKey: String
)
