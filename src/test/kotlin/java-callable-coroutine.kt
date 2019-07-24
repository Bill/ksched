import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * This is a coroutine---similar to the one in deferred.kt
 * It's usable from other coroutines, but not so much from plain old Java code, since
 * you gotta communicate with it via channels and the interface to those is in terms
 * of suspend functions---not so handy from plain old Java.
 */
fun CoroutineScope.theCoroutine(
        submissionChannel: Channel<String>,
        snapshotRequestChannel: Channel<CompletableDeferred<Collection<String>>>) = launch {
        val submissions = mutableListOf<String>()

        val clearContent = ticker(50)

        while(true) {
            select<Unit> {

                submissionChannel.onReceive {
                    log2("received submission: $it")
                    submissions.add(it)
                }

                snapshotRequestChannel.onReceive {
                    log2("received snapshot request")
                    it.complete(submissions.toList())

                }

                clearContent.onReceive {
                    log2("clearing content")
                    submissions.clear()
                }

            }
        }
    }


/**
 * This is a class for handy access to theCoroutine from Java
 * It's a CoroutineScope so that its public functions have coroutine builders e.g.
 * launch and async, available. Those need a CoroutineContext, which is established
 * in the constructor for this class. In this way the coroutine(s) we construct
 * adhere to the the structured concurrency protocol. Part of that entails ensuring that
 * coroutines we spin up---the primary one in init, and the others in the public
 * functions---are all run in the same coroutine scope.
 *
 * TODO: more words about why it's important to run in a a common coroutine scope
 */
class JavaCallableCoroutineScope(
        override val coroutineContext: CoroutineContext = EmptyCoroutineContext) : CoroutineScope {

    // TODO: figure out if I need to create a new context or if it's ok to reuse parent context directly

    val submissionChannel: Channel<String> = Channel()
    val snapshotRequestChannel: Channel<CompletableDeferred<Collection<String>>> =
            Channel()

    init {
        theCoroutine(submissionChannel, snapshotRequestChannel)
    }

    fun submitStuff(stuff: String) = launch {
        // fire and forget
        submissionChannel.send(stuff)
    }

    fun snapshot() : Collection<String> {
        // launch a coroutine to request and wait for the async result---return that result
        return runBlocking {
            val snapshotDeferred = CompletableDeferred<Collection<String>>()
            log2("sending snapshot request (deferred)")
            snapshotRequestChannel.send(snapshotDeferred)
            val snapshot = snapshotDeferred.await()
            snapshot.also {
                log2("got deferred snapshot value: $snapshot")
            }
        }
    }
}

/**
 * This is a coroutine builder. It's only purpose is to be an extension fn of
 * CoroutineScope, thereby making coroutineContext available, and to use that
 * coroutineContext to chain the created CoroutineScope (JavaCallableCoroutineScope)
 * into the structured concurrency, um, structure.
 */
fun CoroutineScope.javaCallableCoroutine() : JavaCallableCoroutineScope {
    return JavaCallableCoroutineScope(coroutineContext)
}


fun main() = runBlocking {

    val jccs = JavaCallableCoroutineScope()

    val contentProducer = launch {
        while(true) {
            delay(10)
            val content = LocalDateTime.now().toString()
            log2("sending content: $content")
            jccs.submitStuff(content)
        }
    }

    val snapshotter = launch {
        while(true) {
            delay(100)
            log2("sending snapshot request (synchronously)")
            val snapshot = jccs.snapshot()
            log2("got snapshot (synchronously): $snapshot")
        }
    }

    delay(10000)

    coroutineContext.cancelChildren()
}