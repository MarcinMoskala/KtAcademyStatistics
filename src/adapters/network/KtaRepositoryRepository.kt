package adapters.network

import domain.repository.UserRepository
import retrofit2.http.GET
import retrofit2.http.Header

class KtaRepositoryRepository : UserRepository {
    private val api = makeRetrofit("https://gapi.kt.academy/")
        .create(LeanpubCouponsApi::class.java)

    override suspend fun isAdmin(userUuid: String): Boolean =
        "ADMIN" in api.me(userUuid).tags
}

interface LeanpubCouponsApi {
    @GET("user/me")
    suspend fun me(@Header("userUuid") userUuid: String): UserJson
}

data class UserJson(
    val tags: List<String>,
)