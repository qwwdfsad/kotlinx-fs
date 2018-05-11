package kotlinx.fs.core

import java.io.Closeable as JvmCloseable

actual typealias IOException = java.io.IOException
actual typealias Closeable = JvmCloseable

actual typealias InputStream = java.io.InputStream
actual typealias ByteArrayInputStream = java.io.ByteArrayInputStream

actual typealias OutputStream = java.io.OutputStream
actual typealias ByteArrayOutputStream = java.io.ByteArrayOutputStream
