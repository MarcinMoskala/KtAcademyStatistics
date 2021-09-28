package domain

import adapters.rest.DayStatisticsJson
import adapters.rest.PostPageLoadRequest
import adapters.rest.RequiresAdminException
import adapters.rest.StatisticsJson
import domain.repository.PageStatsRepository
import domain.repository.UserRepository
import org.joda.time.LocalDate

class StatisticsService(
    private val statisticsRepository: PageStatsRepository,
    private val userRepository: UserRepository,
) {
    suspend fun addPageLoad(userUuid: String, request: PostPageLoadRequest) {
        statisticsRepository.addPageLoad(userUuid, request.pageKey)
    }

    suspend fun getArticlesStatistics(userUuid: String): StatisticsJson {
        requireAdmin(userUuid)
        return statisticsRepository.getArticlesStatistics()
    }

    suspend fun getStatistics(userUuid: String): StatisticsJson {
        requireAdmin(userUuid)
        return statisticsRepository.getPagesStatistics()
    }

    suspend fun getPageStatistics(userUuid: String, pageKey: String): StatisticsJson {
        requireAdmin(userUuid)
        return statisticsRepository.getPageStatistics(pageKey)
    }

    suspend fun getDayStatistics(userUuid: String, date: LocalDate): DayStatisticsJson {
        requireAdmin(userUuid)
        return statisticsRepository.getDayStatistics(date)
    }

    private suspend fun requireAdmin(userUuid: String) {
        if(!userRepository.isAdmin(userUuid)) {
            throw RequiresAdminException
        }
    }
}