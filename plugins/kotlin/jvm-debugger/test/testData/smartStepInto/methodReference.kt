fun foo(x: Int) = Unit

fun main() {
    val list = listOf(1, 2, 3)
    list.forEach(::foo)<caret>
}

// EXISTS: forEach((T) -> Unit), foo(Int)
