import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import java.lang.System.nanoTime
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    val random = createRandom(1)

    val testCoroutineScope = TestCoroutineScope(
            random = random,
            standardDeviationNanos = TimeUnit.SECONDS.toNanos(30))
    logTime(testCoroutineScope)
    testCoroutineScope.launch { printIntegersWithDelay(10) }

    testCoroutineScope.advanceTimeBy(TimeUnit.MILLISECONDS.convert(8,TimeUnit.MINUTES))
    println("8 minute point")
    logTime(testCoroutineScope)
    testCoroutineScope.advanceUntilIdle()
    logTime(testCoroutineScope)
}

private fun logTime(testCoroutineScope: TestCoroutineScope) {
    println("virtual time: ${TimeUnit.SECONDS.convert(testCoroutineScope.currentTime, TimeUnit.MILLISECONDS)} seconds")
}

suspend fun printIntegersWithDelay(count: Int) {
    repeat(count)
    {
        delay(TimeUnit.MILLISECONDS.convert(1,TimeUnit.MINUTES))
        println("$it in thread ${Thread.currentThread().id}")
    }
}

private val exploreStateSpace = true

internal fun createRandom(trySeed: Long): Random {

    val actualSeed: Long

    if (exploreStateSpace)
        actualSeed = nanoTime()
    else
        actualSeed = trySeed

    println(String.format("using seed %,d", actualSeed))

    try {
        return SecureRandom.getInstance("SHA1PRNG").also {
            it.setSeed(actualSeed)
        }
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError(e)
    }

}
