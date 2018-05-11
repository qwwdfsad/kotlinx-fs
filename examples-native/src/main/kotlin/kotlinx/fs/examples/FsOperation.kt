package kotlinx.fs.examples

interface FsOperation {
    fun execute(args: Map<String, String>): Unit
    fun getName(): String
}