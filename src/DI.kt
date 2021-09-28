package academy.kt

import academy.kt.domain.repository.RandomUuidProvider
import academy.kt.domain.repository.RealTimeProvider
import academy.kt.domain.repository.TimeProvider
import academy.kt.domain.repository.UuidProvider
import adapters.mongo.MongoPageStatsRepository
import adapters.network.KtaRepositoryRepository
import domain.StatisticsService
import domain.repository.PageStatsRepository
import domain.repository.UserRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

@Suppress("RemoveExplicitTypeArguments")
fun productionModule(): Module = module {
    // Repositories and clients
    single<TimeProvider> { RealTimeProvider() }
    single<UuidProvider> { RandomUuidProvider() }
    single<CoroutineDatabase> {
        KMongo.createClient("mongodb+srv://admin:cde34RFVBGHN@cluster0.ebez3.mongodb.net/<dbname>?retryWrites=true&w=majority")
            .coroutine
            .getDatabase("statistics")
    }
    single<PageStatsRepository> {
        MongoPageStatsRepository(
            get<CoroutineDatabase>().getCollection("pageLoads"),
            get(),
            get()
        )
    }
    single<UserRepository> {
        KtaRepositoryRepository()
    }
    single { StatisticsService(get(), get()) }
}
