package kotlinx.fs.core


actual open class IOException actual constructor(message: String) : Exception(message) {
    actual constructor() : this("JS IO Exception")
}
