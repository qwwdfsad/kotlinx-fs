package kotlinx.fs.core


actual open class IOException actual constructor(message: String) : Exception(message) {
    actual constructor() : this("JS IO Exception")
}

actual typealias InputStream = kotlinx.fs.core.internal.InputStream
actual typealias ByteArrayInputStream = kotlinx.fs.core.internal.KByteArrayInputStream

actual typealias OutputStream = kotlinx.fs.core.internal.OutputStream
actual typealias ByteArrayOutputStream = kotlinx.fs.core.internal.ByteArrayOutputStream
