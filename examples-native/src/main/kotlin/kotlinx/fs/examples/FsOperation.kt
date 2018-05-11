package kotlinx.fs.examples

interface FsOperation {
    fun execute(path: String, args: Map<String, String>): Unit
    fun getName(): String
}