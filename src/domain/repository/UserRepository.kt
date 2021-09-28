package domain.repository

interface UserRepository {
    suspend fun isAdmin(userUuid: String): Boolean
}
