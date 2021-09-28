package integration

import academy.kt.app
import academy.kt.domain.repository.TimeProvider
import academy.kt.domain.repository.UuidProvider
import academy.kt.productionModule
import adapters.mongo.MongoPageStatsRepository
import domain.repository.PageLoad
import domain.repository.PageStatsRepository
import domain.repository.UserRepository
import fakes.FakeTimeProvider
import fakes.FakeUuidProvider
import fakes.InMemoryUserRepository
import io.ktor.application.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import kotlin.test.AfterTest

abstract class IntegrationTest {
    private val database = FakeMongoDb.database

    private val testPageLoadsCollection = database.getCollection<PageLoad>("testPageLoads")

    protected val timeProvider = FakeTimeProvider()
    protected val uuidProvider = FakeUuidProvider()
    protected val userRepository = InMemoryUserRepository()

    protected val statisticsRepository =
        MongoPageStatsRepository(testPageLoadsCollection, timeProvider, uuidProvider)

    @Before
    fun before() = runBlocking<Unit> {
        timeProvider.clean()
        uuidProvider.clean()
        testPageLoadsCollection.deleteMany()
    }

    @AfterTest
    fun cleanup() = runBlocking {
        stopKoin()
    }

    protected fun integrationModule(): List<Module> = productionModule() + module {
        single<TimeProvider>(override = true) { timeProvider }
        single<UuidProvider>(override = true) { uuidProvider }
        single<PageStatsRepository>(override = true) { statisticsRepository }
        single<UserRepository>(override = true) { userRepository }
    }

    protected fun integrationModule(app: Application) = with(app) {
        install(Koin) {
            modules(integrationModule())
        }
        app()
    }
}
