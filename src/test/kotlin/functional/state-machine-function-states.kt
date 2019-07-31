package functional

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

@FunctionalInterface
interface State {
    /**
     * Process the first input from the channel(s) [perform a side-effect] and return a new functional.State
     */
    suspend fun process(
            pushes: ReceiveChannel<Unit>,
            coins: ReceiveChannel<Unit>): State
}

/*
 Must explicitly provide fun return type (State) otherwise ::locked ref causes compile error:

 "Type checking has run into a recursive problem..."
 */
suspend fun locked(pushes: ReceiveChannel<Unit>, coins: ReceiveChannel<Unit>): State =
        select<State> {
            pushes.onReceive {
                println("push blocked---functional.turnstile is functional.locked")
                ::locked as State
            }
            coins.onReceive {
                println("coins received---unlocking")
                ::unlocked as State
            }
        }

suspend fun unlocked(pushes: ReceiveChannel<Unit>, coins: ReceiveChannel<Unit>): State =
        select<State> {
            pushes.onReceive {
                println("Opened!")
                ::locked as State
            }
            coins.onReceive {
                println("Already functional.unlocked---returning coins")
                ::unlocked as State
            }
        }

private fun CoroutineScope.turnstile(
        pushes: ReceiveChannel<Unit>,
        coins: ReceiveChannel<Unit>) = launch {

    /*
     Fails at runtime:

     Exception in thread "main" java.lang.ClassCastException: class functional.State_machine_function_statesKt$turnstile$1$state$1 cannot be cast to class functional.State

     */
    var state = ::locked as State

    while (true) {
        state = state.process(pushes, coins)
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
    turnstile(pushes, coins)
}