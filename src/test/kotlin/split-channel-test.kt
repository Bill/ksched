import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.stream.Collectors
import java.util.stream.Stream

class SplitChannelTest {

    class SplitterSubscriber(
            val name: String,
            period: Long,
            timeUnit: TimeUnit,
            val source: ReceiveChannel<Int>,
            coroutineScope: CoroutineScope) {

        val seen: MutableList<Int> = ArrayList()
        val periodMillis: Long = timeUnit.toMillis(period)

        init {
            coroutineScope.doSteps()
        }

        /*
         Periodically consume elements from upstream
         */
        private fun CoroutineScope.doSteps() = launch {

            delay(periodMillis)

            log("requesting 1")

            for (item in source) {

                log("got item: $item")

                seen.add(item)

                delay(periodMillis)

                log("requesting 1")
            }
            log("complete.")
        }

        private fun log(s: String) {
            println("$name $s (thread: ${Thread.currentThread().id})")
        }
    }

    @Disabled
    @Test
    fun split2WithRealTimeMultiThreaded() = runBlocking {

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        runBlocking(coroutineScope.coroutineContext) {

            val subscribers = doSplittingStuff()

            delay(10_000L)

            validate(subscribers)
        }

    }

    private val exploreStateSpace = false

    @ParameterizedTest
    @ValueSource(longs = [225_912_776_299_004L])
    fun split2In9VirtualSeconds(seed: Long) {

        val random = createRandom(seed)

        val testCoroutineScope = TestCoroutineScope(
                random = random,
                standardDeviation = 20,
                standardDeviationUnit = MILLISECONDS)

        testCoroutineScope.runBlockingTest {

            pauseDispatcher {

                val subscribers = doSplittingStuff()

                runCurrent()

                advanceTimeBy(MILLISECONDS.convert(10, SECONDS))

                validate(subscribers)
            }
        }

    }

    private fun validate(subscribers: Pair<SplitterSubscriber,SplitterSubscriber>) {
        val(fast, slow) = subscribers

        /*
         Have to turn the stream into an iterable to compare it due to AssertJ 'cause
         https://github.com/joel-costigliola/assertj-core/issues/1545
         */
        val asList = testSequence().collect(Collectors.toList<Int>())
        assertThat(fast.seen).`as`("${fast.name} subscriber sees correct stream of items").isEqualTo(asList)
        assertThat(slow.seen).`as`("${slow.name} subscriber sees correct stream of items").isEqualTo(asList)
    }


    private fun testSequence(): Stream<Int> {
        return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    fun CoroutineScope.doSplittingStuff(): Pair<SplitterSubscriber,SplitterSubscriber> {

        /*

         !! LOOKIE HERE !! this is what we're testing

         publish(2) creates the ConnectableFlux that will start producing to all subscribers
         after the second subscription arrives

         */
        val topic = BroadcastChannel<Int>(3)

        /*
         Open subscriptions before launching any coroutines so we're sure each coroutine sees
         all elements
         */
        val subscription1 = topic.openSubscription()
        val subscription2 = topic.openSubscription()

        val fast = SplitterSubscriber("Fast!", 100, MILLISECONDS, subscription1, this)
        val slow = SplitterSubscriber("slow ", 1, SECONDS, subscription2, this)

        launch { producer(topic) }

        return Pair(fast,slow)

    }

    suspend fun producer(topic: SendChannel<Int>) {
        println("about to start broadcasting")
        for (item in testSequence()) {
            println("broadcasting item $item")
            topic.send(item)
        }
        topic.close()
    }

    fun createRandom(trySeed: Long): Random {

        val actualSeed: Long

        if (exploreStateSpace)
            actualSeed = System.nanoTime()
        else
            actualSeed = trySeed

        println("using seed " + readableSeed(actualSeed))

        try {
            return SecureRandom.getInstance("SHA1PRNG").also {
                it.setSeed(actualSeed)
            }
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }

    }

    fun readableSeed(seed: Long): String {
        return String.format("%,d",seed).replace(',','_') + "L"
    }

    fun logTime(testCoroutineScope: TestCoroutineScope) {
        println("virtual time: ${SECONDS.convert(testCoroutineScope.currentTime, MILLISECONDS)} seconds")
    }

}

