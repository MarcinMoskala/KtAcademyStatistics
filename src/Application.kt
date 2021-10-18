package academy.kt

import adapters.rest.setupEndpoints
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.locations.*
import io.sentry.Sentry
import io.sentry.SentryOptions
import org.koin.ktor.ext.Koin

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(Koin) {
        modules(productionModule())
    }
    app()
}

fun Application.app() {
    install(CORS) {
        anyHost()
        header(HttpHeaders.XForwardedProto)
        method(HttpMethod.Options)
        method(HttpMethod.Delete)
        method(HttpMethod.Post)
        method(HttpMethod.Patch)
        method(HttpMethod.Put)
        header("userUuid")
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    install(ContentNegotiation) {
        register(Json, GsonConverter)
    }

    install(CallLogging)

    setupEndpoints()
}
