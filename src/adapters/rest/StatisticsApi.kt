package adapters.rest

import org.joda.time.LocalDate

data class StatisticsJson(
    val perPage: List<PageStatisticsJson>,
    val perDay: List<DailyStatisticsJson>
)

data class DailyStatisticsJson(
    val date: LocalDate,
    val viewsCount: Int,
    val usersCount: Int?,
)

data class PageStatisticsJson(
    val pageKey: String,
    val last30DaysViewsCount: Int,
    val last30DaysUsersCount: Int,
    val lastYearViewsCount: Int? = null,
    val lastYearUsersCount: Int? = null,
)

data class DayStatisticsJson(
    val perPage: List<DayPageStatisticsJson>
)

data class DayPageStatisticsJson(
    val pageKey: String,
    val viewsCount: Int,
    val usersCount: Int,
)