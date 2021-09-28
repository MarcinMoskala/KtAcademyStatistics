package academy.kt.domain.repository

import java.util.*

interface UuidProvider {
    fun next(): String
}

class RandomUuidProvider : UuidProvider {
    override fun next(): String = UUID.randomUUID().toString()
}