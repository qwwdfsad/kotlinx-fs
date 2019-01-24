package kotlinx.fs.core

expect open class IOException : Exception {
    constructor()
    constructor(message: String)
}
