import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
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

typealias Receivers = Pair<ReceiveChannel<List<Int>>,ReceiveChannel<List<Int>>>

class SplitChannelTest {

    fun CoroutineScope.splitterSubscriber(
            name: String,
            period: Long,
            timeUnit: TimeUnit,
            source: ReceiveChannel<Int>,
            seenChannel: SendChannel<List<Int>>): Job = launch {

        fun log(s: String) {
            println("$name $s (thread: ${Thread.currentThread().id})")
        }

        val periodMillis: Long = timeUnit.toMillis(period)

        val seen: MutableList<Int> = ArrayList()

        delay(periodMillis)

        log("requesting 1")

        for (item in source) {

            log("got item: $item")

            seen.add(item)

            delay(periodMillis)

            log("requesting 1")
        }
        seenChannel.send(seen)

        log("complete.")
    }

    @Disabled
    @Test
    fun split2WithRealTimeMultiThreaded() = runBlocking {

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        runBlocking(coroutineScope.coroutineContext) {

            val subscribers = doSplittingStuff(100, MILLISECONDS, 1, SECONDS)

            delay(10_000L)

            validate(subscribers)
        }

    }

    private val exploreStateSpace = false

    @ParameterizedTest
    @ValueSource(longs = [19_328_895_268_198L])
    fun split2In9VirtualSeconds(seed: Long) {

        val random = createRandom(seed)

        val testCoroutineScope = TestCoroutineScope(
                random = random,
                standardDeviation = 20,
                standardDeviationUnit = MILLISECONDS)

        testCoroutineScope.runBlockingTest {

            pauseDispatcher {

                val subscribers = doSplittingStuff(100, MILLISECONDS, 1, SECONDS)

                runCurrent()

                advanceTimeBy(MILLISECONDS.convert(11, SECONDS))

                validate(subscribers)
            }
        }

    }

    private fun validate(subscribers: Receivers) {
        val(fast, slow) = subscribers

        val fastResult = fast.poll()
        val slowResult = slow.poll()

        assertThat(fastResult).`as`("FAST! subscriber got all items in time").isNotNull()
        assertThat(slowResult).`as`("slow  subscriber got all items in time").isNotNull()

        /*
         Have to turn the stream into an iterable to compare it due to AssertJ 'cause
         https://github.com/joel-costigliola/assertj-core/issues/1545
         */
        val asList = testSequence().collect(Collectors.toList<Int>())
        assertThat(fastResult).`as`("FAST! subscriber sees correct items").isEqualTo(asList)
        assertThat(slowResult).`as`("slow  subscriber sees correct items").isEqualTo(asList)
    }


    private fun testSequence(): Stream<Int> {
        return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    fun CoroutineScope.doSplittingStuff(
            fastPeriod: Long,
            fastTimeUnit: TimeUnit,
            slowPeriod: Long,
            slowTimeUnit: TimeUnit):
            Receivers {

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

        return Pair(Channel<List<Int>>(1), Channel<List<Int>>(1))
                .also {

                    splitterSubscriber(
                            "Fast!",
                            fastPeriod,
                            fastTimeUnit,
                            topic.openSubscription(),
                            it.first)

                    splitterSubscriber(
                            "slow ",
                            slowPeriod,
                            slowTimeUnit,
                            topic.openSubscription(),
                            it.second)

                    producer(topic)
                }
    }

    fun CoroutineScope.producer(topic: SendChannel<Int>) = launch {
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

}

