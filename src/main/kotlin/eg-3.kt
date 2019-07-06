import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    launch { // launch new coroutine and keep a reference to its Job
        doWorld()
    }
    println("Hello,")
}

private suspend fun doWorld() {
    delay(1000L)
    println("World!")
}