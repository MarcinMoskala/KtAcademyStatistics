package adapters.mongo

import academy.kt.domain.repository.TimeProvider
import academy.kt.domain.repository.UuidProvider
import academy.kt.toDateTime
import academy.kt.toJavaInstant
import adapters.rest.*
import com.mongodb.client.model.Projections
import domain.repository.PageLoad
import domain.repository.PageStatsRepository
import kotlinx.coroutines.runBlocking
import org.bson.BsonDocument
import org.bson.BsonElement
import org.bson.BsonString
import org.joda.time.LocalDate
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.aggregate
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoPageStatsRepository(
    private val collection: CoroutineCollection<PageLoad>,
    private val timeProvider: TimeProvider,
    private val uuidProvider: UuidProvider
) : PageStatsRepository {

    override suspend fun addPageLoad(userUuid: String, pageKey: String) {
        collection.insertOne(PageLoad(uuidProvider.next(), userUuid, timeProvider.now(), pageKey))
    }

    override suspend fun getPageStatistics(pageKey: String): StatisticsJson {
        val today = timeProvider.now().toDateTime().toLocalDate()
        val last30dStatistics = pageViewsPerPage(pageKey = pageKey, from = today.minusDays(30), to = today)
        return StatisticsJson(
            perPage = last30dStatistics
                .map {
                    PageStatisticsJson(
                        pageKey = it._id,
                        last30DaysViewsCount = it.viewsCount,
                        last30DaysUsersCount = it.usersCount ?: 0,
                    )
                },
            perDay = pageViewsPerDay(pageKey = pageKey, from = today.minusDays(30))
        )
    }

    override suspend fun getDayStatistics(date: LocalDate): DayStatisticsJson {
        val statistics = pageViewsPerPage(from = date, to = date.plusDays(1))
        return DayStatisticsJson(
            perPage = statistics
                .map {
                    DayPageStatisticsJson(
                        pageKey = it._id,
                        viewsCount = it.viewsCount,
                        usersCount = it.usersCount ?: 0,
                    )
                }
        )
    }

    override suspend fun getPagesStatistics(): StatisticsJson {
        val today = timeProvider.now().toDateTime().toLocalDate()
        val last30dStatistics = pageViewsPerPage(from = today.minusDays(30), to = today)
        return StatisticsJson(
            perPage = last30dStatistics
                .map {
                    PageStatisticsJson(
                        pageKey = it._id,
                        last30DaysViewsCount = it.viewsCount,
                        last30DaysUsersCount = it.usersCount ?: 0,
                    )
                },
            perDay = pageViewsPerDay(from = today.minusDays(30))
        )
    }

    override suspend fun getArticlesStatistics(): StatisticsJson {
        val today = timeProvider.now().toDateTime().toLocalDate()
        val articlePageKeyRegex = "^kta-article-.*"

        val last30dStatistics =
            pageViewsPerPage(from = today.minusDays(30), to = today, pageKeyRegex = articlePageKeyRegex)
                .associateBy { it._id }

        val lastYearStatistics = pageViewsPerPage(from = today.minusDays(365), pageKeyRegex = articlePageKeyRegex)

        return StatisticsJson(
            perPage = lastYearStatistics
                .map {
                    val last30DayStats = last30dStatistics[it._id]
                    PageStatisticsJson(
                        pageKey = it._id,
                        lastYearViewsCount = it.viewsCount,
                        lastYearUsersCount = it.usersCount ?: 0,
                        last30DaysViewsCount = last30DayStats?.viewsCount ?: 0,
                        last30DaysUsersCount = last30DayStats?.usersCount ?: 0,
                    )
                },
            perDay = pageViewsPerDay(from = today.minusDays(30), pageKeyRegex = articlePageKeyRegex)
        )
    }

    data class PageViewsResult(val _id: String, val viewsCount: Int, val usersCount: Int?, val users: List<String>?)

    private suspend fun pageViewsPerPage(
        from: LocalDate? = null,
        to: LocalDate? = null,
        pageKeyRegex: String? = null,
        pageKey: String? = null,
    ): List<PageViewsResult> {
        val filters = listOfNotNull(
            pageKeyRegex?.let { PageLoad::pageKey regex pageKeyRegex },
            pageKey?.let { PageLoad::pageKey eq pageKey },
            from?.let { PageLoad::timestamp gte from.toDateTimeAtStartOfDay().toJavaInstant() },
            to?.let { PageLoad::timestamp lte to.toDateTimeAtStartOfDay().toJavaInstant() },
        )
        return collection
            .aggregate<PageViewsResult>(
                match(and(filters)),
                group(
                    PageLoad::pageKey,
                    PageViewsResult::viewsCount.sum(1),
                    PageViewsResult::users.addToSet(PageLoad::userUuid)
                ),
                project(
                    PageLoad::pageKey to 1,
                    PageViewsResult::viewsCount to 1,
                    PageViewsResult::usersCount to Projections.computed("\$size", "\$users")
                )
            ).toList()
            .sortedByDescending { it.viewsCount }
    }

    private suspend fun pageViewsPerDay(
        from: LocalDate? = null,
        to: LocalDate? = null,
        pageKeyRegex: String? = null,
        pageKey: String? = null,
    ): List<DailyStatisticsJson> {
        val filters = listOfNotNull(
            pageKeyRegex?.let { PageLoad::pageKey regex pageKeyRegex },
            pageKey?.let { PageLoad::pageKey eq pageKey },
            from?.let { PageLoad::timestamp gte from.toDateTimeAtStartOfDay().toJavaInstant() },
            to?.let { PageLoad::timestamp lte to.toDateTimeAtStartOfDay().toJavaInstant() },
        )
        return collection
            .aggregate<PageViewsResult>(
                match(and(filters)),
                group(
                    BsonDocument(
                        "\$dateToString", BsonDocument(
                            listOf(
                                BsonElement("format", BsonString("%Y-%m-%d")),
                                BsonElement("date", BsonString("\$timestamp"))
                            )
                        )
                    ),
                    PageViewsResult::viewsCount.sum(1),
                    PageViewsResult::users.addToSet(PageLoad::userUuid)
                ),
                project(
                    PageLoad::pageKey to 1,
                    PageViewsResult::viewsCount to 1,
                    PageViewsResult::usersCount to Projections.computed("\$size", "\$users")
                )
            ).toList()
            .map { DailyStatisticsJson(LocalDate.parse(it._id), it.viewsCount, it.usersCount) }
            .sortedByDescending { it.date }
    }
}

fun main() = runBlocking<Unit> {
    val collection =
        KMongo.createClient("mongodb+srv://marcinmoskala:cde34RFVBGHN@cluster0-w3cil.mongodb.net/<dbname>?retryWrites=true&w=majority")
            .coroutine
            .getDatabase("ktacademy")
            .getCollection<PageLoad>("pageLoads")

    val today = LocalDate.now()

    data class Result(val _id: String, val viewsCount: Int, val usersCount: Int?, val users: List<String>?)

    val viewsLast30Days = collection
        .aggregate<MongoPageStatsRepository.PageViewsResult>(
            match( // articles from last 30 days
                and(
                    PageLoad::pageKey regex "^kta-article-.*",
                    PageLoad::timestamp gte today.minusDays(30).toDateTimeAtStartOfDay().toJavaInstant(),
                    PageLoad::timestamp lte today.toDateTimeAtStartOfDay().toJavaInstant()
                )
            ),
            group(
                BsonDocument(
                    "\$dateToString", BsonDocument(
                        listOf(
                            BsonElement("format", BsonString("%Y-%m-%d")),
                            BsonElement("date", BsonString("\$timestamp"))
                        )
                    )
                ),
                Result::viewsCount.sum(1),
                Result::users.addToSet(PageLoad::userUuid)
            ),
            project(
                PageLoad::pageKey to 1,
                Result::viewsCount to 1,
                Result::usersCount to Projections.computed("\$size", "\$users")
            )
        )
        .toList()
        .map { DailyStatisticsJson(LocalDate.parse(it._id), it.viewsCount, it.usersCount) }

    println()
    println("Res:")
    println(viewsLast30Days)
    println()
    println()
}
