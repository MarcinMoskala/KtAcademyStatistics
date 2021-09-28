package fakes

import TestData.date1
import academy.kt.domain.repository.TimeProvider
import academy.kt.toJavaInstant
import org.joda.time.DateTime
import java.time.Instant

class FakeTimeProvider : TimeProvider {
    private var currentTime = date1

    override fun now(): Instant = currentTime

    fun advanceTimeTo(instant: Instant) {
        currentTime = instant
    }

    fun advanceTimeTo(dateTime: DateTime) {
        currentTime = dateTime.toJavaInstant()
    }

    fun advanceTimeByDays(days: Int) {
        currentTime = currentTime.plusSeconds(1L * days * 60 * 60 * 24)
    }

    fun clean() {
        currentTime = date1
    }

    fun advanceTime() {
        currentTime = currentTime.plusSeconds(10)
    }
}