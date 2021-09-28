package fakes

import domain.repository.UserRepository

class InMemoryUserRepository : UserRepository {
    private var adminUuids = listOf<String>()

    override suspend fun isAdmin(userUuid: String): Boolean = userUuid in adminUuids

    fun adminExists(uuid: String) {
        adminUuids += uuid
    }

    fun clear() {
        adminUuids = emptyList()
    }
}
