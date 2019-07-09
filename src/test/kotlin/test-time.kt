import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.System.nanoTime
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit


class FooTest {
    private val exploreStateSpace = true

    @Test
    fun foo() {
        val random = createRandom(1)

        val testCoroutineScope = TestCoroutineScope(
                random = random,
                standardDeviation = 30,
                standardDeviationUnit = TimeUnit.SECONDS)
        logTime(testCoroutineScope)

        testCoroutineScope.launch {printIntegersWithDelay(10)}

        testCoroutineScope.advanceTimeBy(TimeUnit.MILLISECONDS.convert(8,TimeUnit.MINUTES))
        println("8 minute point")
        logTime(testCoroutineScope)
        testCoroutineScope.advanceUntilIdle()
        logTime(testCoroutineScope)
    }

    suspend fun printIntegersWithDelay(count: Int) {
        repeat(count)
        {
            delay(TimeUnit.MILLISECONDS.convert(1,TimeUnit.MINUTES))
            println("$it in thread ${Thread.currentThread().id}")
        }
    }

    fun logTime(testCoroutineScope: TestCoroutineScope) {
        println("virtual time: ${TimeUnit.SECONDS.convert(testCoroutineScope.currentTime, TimeUnit.MILLISECONDS)} seconds")
    }

    fun createRandom(trySeed: Long): Random {

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

}