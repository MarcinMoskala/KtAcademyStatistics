package adapters.rest

import domain.StatisticsService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.koin.ktor.ext.inject

fun Application.setupEndpoints() {
    routing {
        val facade: StatisticsService by inject()
        get {
            call.respond("Works :)")
        }

        route("/page-load") {
            post {
                val userUuid = headerUUID()
                val body = call.receive<PostPageLoadRequest>()
                facade.addPageLoad(userUuid, body)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/statistics") {
            get {
                val userUuid = headerUUID()
                call.respond(facade.getStatistics(userUuid))
            }
            get("/articles") {
                val userUuid = headerUUID()
                call.respond(facade.getArticlesStatistics(userUuid))
            }
            get("/{pageKey}") {
                val userUuid = headerUUID()
                val pageKey = requireParameter("pageKey")
                call.respond(facade.getPageStatistics(userUuid, pageKey))
            }
            get("/day/{day}") {
                val userUuid = headerUUID()
                val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
                val day = LocalDate.parse(requireParameter("day"), formatter)
                call.respond(facade.getDayStatistics(userUuid, day))
            }
        }
    }

    install(StatusPages) {
        exception<UserUuidRequired> { cause ->
            log.error(cause.message, cause)
            call.respond(HttpStatusCode.BadRequest, "userUuid required in the header")
        }
        exception<RequiresAdminException> { cause ->
            log.error(cause.message, cause)
            call.respond(HttpStatusCode.Forbidden, "User needs to be an admin")
        }
        exception<MissingParameterError> { cause ->
            log.error(cause.message, cause)
            call.respond(HttpStatusCode.BadGateway, "Missing parameter ${cause.name}")
        }
    }
}

object UserUuidRequired : Throwable()

object RequiresAdminException : Throwable("Requires admin")

class MissingParameterError(val name: String?) : Throwable()

class PostPageLoadRequest(
    val pageKey: String
)

fun PipelineContext<Unit, ApplicationCall>.requireParameter(name: String) =
    call.parameters[name] ?: throw MissingParameterError(name)

fun PipelineContext<Unit, ApplicationCall>.headerUUID(): String =
    headerUUIDorNull() ?: throw UserUuidRequired

fun PipelineContext<Unit, ApplicationCall>.headerUUIDorNull(): String? =
    call.request.header("userUuid")
