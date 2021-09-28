package integration

import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.memory.MemoryBackend
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.net.InetSocketAddress

object FakeMongoDb {
    val server = MongoServer(MemoryBackend())
    val serverAddress: InetSocketAddress = server.bind().also {
        Runtime.getRuntime().addShutdownHook(Thread { server.shutdown() })
    }

    val database: CoroutineDatabase = KMongo
        .createClient("mongodb://${serverAddress.address.hostAddress}:${serverAddress.port}").coroutine
        .getDatabase("test")
}
