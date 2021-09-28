package academy.kt.domain.repository

import java.time.Instant

interface TimeProvider {
    fun now(): Instant
}

class RealTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
}