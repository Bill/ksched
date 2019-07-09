import java.util.stream.Stream

fun main(args: Array<String>) {
    println("about to start broadcasting")
    for (item in testSequence()) {
        println("broadcasting item $item")
    }
}

fun testSequence(): Stream<Int> {
    return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
}
