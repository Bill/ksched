import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.time.LocalDateTime

fun CoroutineScope.producer() = produce<String> {
    while (true) {
        delay(300)
        send(LocalDateTime.now().toString())
    }
}

fun <T> CoroutineScope.timeWindow(producer: ReceiveChannel<T>, delayMillis: Long) = produce<List<T>> {

    val ticker = ticker(delayMillis)

    var seen = mutableListOf<T>()

    while (true) {

        select<Unit> {
            // <Unit> means that this select expression does not produce any result

            producer.onReceive {
                // this is the first select clause
                seen.add(it)
            }

            ticker.onReceive {
                println(" sending window: ${seen}")
                this@produce.send(seen)
                seen = mutableListOf<T>()
            }
        }
    }
}

fun main() = runBlocking<Unit> {
    val windows = timeWindow(producer(), 900)
    repeat(5) {
        val window = windows.receive()
        println("received window: $window")
    }
    coroutineContext.cancelChildren()
}
