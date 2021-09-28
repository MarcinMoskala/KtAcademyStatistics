package integration

import TestData.aPageKey
import TestData.aPageKey2
import TestData.aUserUuid
import TestData.aUserUuid2
import TestData.aUserUuid3
import TestData.anArticlePageKey
import TestData.anArticlePageKey2
import TestData.date1
import academy.kt.toDateTime
import adapters.rest.*
import integration.traits.PageLoadsTrait
import io.ktor.server.testing.*
import org.joda.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class StatisticsApiTests : IntegrationTest(), PageLoadsTrait {
    private val date1Local: LocalDate = date1.toDateTime().toLocalDate()

    @Test
    fun `Pages stats complex case`() = withTestApplication(::integrationModule) {
        userRepository.adminExists(aUserUuid3)
        timeProvider.advanceTimeTo(date1)

        postPageLoad(aUserUuid, aPageKey)
        postPageLoad(aUserUuid2, aPageKey2)

        timeProvider.advanceTimeByDays(1)

        postPageLoad(aUserUuid, aPageKey)
        postPageLoad(aUserUuid, aPageKey)
        postPageLoad(aUserUuid, aPageKey2)
        postPageLoad(aUserUuid2, aPageKey2)

        timeProvider.advanceTimeByDays(1)

        postPageLoad(aUserUuid, aPageKey)
        postPageLoad(aUserUuid2, aPageKey)

        assertEquals(
            DayStatisticsJson(
                perPage = listOf(
                    DayPageStatisticsJson(aPageKey, 2, 1),
                    DayPageStatisticsJson(aPageKey2, 2, 2),
                ),
            ),
            getDayStats(aUserUuid3, date1Local.plusDays(1))
        )

        assertEquals(
            StatisticsJson(
                perPage = listOf(
                    PageStatisticsJson(aPageKey, 3, 1),
                ),
                perDay = listOf(
                    DailyStatisticsJson(date1Local.plusDays(2), 2, 2),
                    DailyStatisticsJson(date1Local.plusDays(1), 2, 1),
                    DailyStatisticsJson(date1Local, 1, 1),
                )
            ),
            getPageStats(aUserUuid3, aPageKey)
        )

        assertEquals(
            StatisticsJson(
                perPage = listOf(
                    PageStatisticsJson(aPageKey2, 3, 2),
                ),
                perDay = listOf(
                    DailyStatisticsJson(date1Local.plusDays(1), 2, 2),
                    DailyStatisticsJson(date1Local, 1, 1),
                )
            ),
            getPageStats(aUserUuid3, aPageKey2)
        )

        assertEquals(
            StatisticsJson(
                perPage = listOf(
                    PageStatisticsJson(aPageKey, 3, 1),
                    PageStatisticsJson(aPageKey2, 3, 2),
                ),
                perDay = listOf(
                    DailyStatisticsJson(date1Local.plusDays(2), 2, 2),
                    DailyStatisticsJson(date1Local.plusDays(1), 4, 2),
                    DailyStatisticsJson(date1Local, 2, 2),
                )
            ),
            getStats(aUserUuid3)
        )
    }

    @Test
    fun `Articles complex case`() = withTestApplication(::integrationModule) {
        userRepository.adminExists(aUserUuid3)
        timeProvider.advanceTimeTo(date1)

        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid2, anArticlePageKey2)

        timeProvider.advanceTimeByDays(1)

        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid, anArticlePageKey2)
        postPageLoad(aUserUuid2, anArticlePageKey2)

        timeProvider.advanceTimeByDays(1)

        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid2, anArticlePageKey)

        assertEquals(
            StatisticsJson(
                perPage = listOf(
                    PageStatisticsJson(anArticlePageKey, 3, 1, 5, 2),
                    PageStatisticsJson(anArticlePageKey2, 3, 2, 3, 2),
                ),
                perDay = listOf(
                    DailyStatisticsJson(date1Local.plusDays(2), 2, 2),
                    DailyStatisticsJson(date1Local.plusDays(1), 4, 2),
                    DailyStatisticsJson(date1Local, 2, 2),
                )
            ),
            getArticlesStats(aUserUuid3)
        )
    }

    @Test
    fun `Articles complex case with outdated loads`() = withTestApplication(::integrationModule) {
        userRepository.adminExists(aUserUuid3)
        timeProvider.advanceTimeTo(date1)

        postPageLoad(aUserUuid2, anArticlePageKey)
        postPageLoad(aUserUuid2, anArticlePageKey2)

        timeProvider.advanceTimeByDays(60)

        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid, anArticlePageKey)
        postPageLoad(aUserUuid2, anArticlePageKey)
        postPageLoad(aUserUuid, anArticlePageKey2)

        timeProvider.advanceTimeByDays(2)

        assertEquals(
            StatisticsJson(
                perPage = listOf(
                    PageStatisticsJson(anArticlePageKey, 3, 2, 4, 2),
                    PageStatisticsJson(anArticlePageKey2, 1, 1, 2, 2),
                ),
                perDay = listOf(
                    DailyStatisticsJson(date1Local.plusDays(60), 4, 2),
                )
            ),
            getArticlesStats(aUserUuid3)
        )
    }
}
