package academy.kt

import com.github.benmanes.caffeine.cache.Cache
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.ktor.application.*
import io.ktor.content.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.lang.reflect.Type
import java.time.Instant
import kotlin.reflect.jvm.jvmErasure
import kotlin.text.Charsets
import io.ktor.response.respond as respondAny

val globalGson by lazy {
    Gson().newBuilder()
        .registerTypeAdapter(DateTime::class.java, DateTimeDeserializer())
        .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        .serializeNulls().create()
}

inline fun <reified T> String.fromJson(): T? = try {
    globalGson.fromJson(this, object : TypeToken<T>() {}.type)
} catch (e: JsonSyntaxException) {
    null
}

inline fun <reified T> T.toJson(): String = globalGson.toJson(this, T::class.java)

//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any?> = convertFromTo()

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T = convertFromTo()

//convert an object of type I to type O
inline fun <I, reified O> I.convertFromTo(): O {
    val json = globalGson.toJson(this)
    return globalGson.fromJson(json, object : TypeToken<O>() {}.type)
}

class DateTimeDeserializer : JsonDeserializer<DateTime?>, JsonSerializer<DateTime?> {
    @Throws(JsonParseException::class)
    override fun deserialize(je: JsonElement, type: Type?, jdc: JsonDeserializationContext?): DateTime? {
        return if (je.asString.isEmpty()) null else DATE_TIME_FORMATTER.parseDateTime(je.asString)
    }

    override fun serialize(src: DateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(if (src == null) "" else DATE_TIME_FORMATTER.print(src))
    }

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = ISODateTimeFormat.dateTime()
    }
}

class LocalDateDeserializer : JsonDeserializer<LocalDate?>, JsonSerializer<LocalDate?> {
    @Throws(JsonParseException::class)
    override fun deserialize(je: JsonElement, type: Type?, jdc: JsonDeserializationContext?): LocalDate? {
        return if (je.asString.isEmpty()) null else LocalDate.parse(je.asString, LOCAL_DATE_FORMAT)
    }

    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(if (src == null) "" else src.toString(LOCAL_DATE_FORMAT))
    }

    companion object {
        private val LOCAL_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd")
    }
}

object GsonConverter : ContentConverter {
    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any,
    ): Any {
        return TextContent(globalGson.toJson(value), contentType.withCharset(context.call.suitableCharset()))
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val reader = (context.call.request.contentCharset() ?: Charsets.UTF_8).newDecoder()
            .decode(channel.readRemaining()).reader()
        return globalGson.fromJson(reader, request.typeInfo.jvmErasure.javaObjectType)
    }
}

fun DateTime.toJavaInstant(): Instant = Instant.ofEpochMilli(this.millis)
fun LocalDate.toDateTimeAtEndOfDay(): DateTime = plusDays(1).toDateTimeAtStartOfDay().minusMillis(1)
fun Instant.toDateTime(): DateTime = DateTime(this.toEpochMilli())

operator fun LocalDate.contains(timestamp: Instant): Boolean =
    timestamp in this.toDateTimeAtStartOfDay().toJavaInstant()..this.plusDays(1).toDateTimeAtStartOfDay().minusMillis(1)
        .toJavaInstant()

inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String) =
    enumValues<T>().find { it.toString() == value }

suspend inline fun <reified T> ApplicationCall.respond(message: T) =
    respondAny(message.toJson())

fun <T> Cache<String, T>.getC(key: String, build: suspend (key: String) -> T) = get(key) { key ->
    runBlocking { build(key) }
}