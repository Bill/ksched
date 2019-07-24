import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import java.time.LocalDateTime

fun log2(msg:String) {
    println("$msg in thread ${Thread.currentThread()}")
}

fun CoroutineScope.accumulateAndSnapshot(
        content: ReceiveChannel<String>,
        snapshotRequests: ReceiveChannel<CompletableDeferred<Collection<String>>>):
        Job = launch {

    log2("starting accumulateAndSnapshot")
    val gatheredContent = mutableListOf<String>()

    val clearContent = ticker(50)

    while(true) {
        select<Unit> {

            content.onReceive {
                log2("received content: $it")
                gatheredContent.add(it)
            }

            snapshotRequests.onReceive {
                log2("received snapshot request")
                assert(it.complete(gatheredContent.toList()) == true)
            }

            clearContent.onReceive {
                log2("clearing content")
                gatheredContent.clear()
            }
        }
    }
}

fun main() = runBlocking<Unit> {
    val contentProducer = produce {
        while(true) {
            delay(10)
            val content = LocalDateTime.now().toString()
            log2("sending content: $content")
            send(content)
        }
    }

    val snapshotter = produce {
        while(true) {
            delay(100)
            val snapshotDeferred = CompletableDeferred<Collection<String>>()
            log2("sending snapshot request (deferred)")
            send(snapshotDeferred)
            val x = snapshotDeferred.await()
            println("got deferred value: $x")
        }
    }

    val thingy = accumulateAndSnapshot(contentProducer, snapshotter)

    delay(10000)

    coroutineContext.cancelChildren()
}
