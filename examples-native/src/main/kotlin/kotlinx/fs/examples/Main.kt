package kotlinx.fs.examples


fun main(args: Array<String>) {
    Ls.execute(args.lastOrNull() ?: ".", emptyMap())
}
