package kotlinx.fs.core

expect interface Closeable {
    fun close()
}

// TODO stdlib copy-paste :(
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                     // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}
