import functional.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

@FunctionalInterface
private interface State {
    /**
     * Process the first input from the channel(s) [perform a side-effect] and return a new functional.State
     */
    suspend fun process(
            pushes: ReceiveChannel<Unit>,
            coins: ReceiveChannel<Unit>): State
}

private object locked : State {
    override suspend fun process(pushes: ReceiveChannel<Unit>, coins: ReceiveChannel<Unit>): State {
        return select<State> {
            pushes.onReceive {
                println("push blocked---turnstile is functional.locked")
                locked
            }
            coins.onReceive {
                println("coins received---unlocking")
                unlocked
            }
        }
    }
}

private object unlocked : State {
    override suspend fun process(pushes: ReceiveChannel<Unit>, coins: ReceiveChannel<Unit>): State {
        return select<State> {
            pushes.onReceive {
                println("Opened!")
                locked
            }
            coins.onReceive {
                println("Already functional.unlocked---returning coins")
                unlocked
            }
        }
    }
}

private fun CoroutineScope.turnstile(
        pushes: ReceiveChannel<Unit>,
        coins: ReceiveChannel<Unit>) = launch {

    var state: State = locked

    while(true) {
        state = state.process(pushes,coins)
    }
}

fun main() = runBlocking<Unit> {
    val pushes = produce {
        repeat(10) {
            send(Unit)
        }
    }
    val coins = produce {
        repeat(10) {
            send(Unit)
        }
    }
    turnstile(pushes,coins)
}