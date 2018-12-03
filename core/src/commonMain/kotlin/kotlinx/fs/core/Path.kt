package kotlinx.fs.core

expect interface Path {
    fun getParent(): Path?
    fun getFileName(): Path?
    fun isAbsolute(): Boolean
    fun getNameCount(): Int
    fun getName(index: Int): Path
    fun resolve(other: Path): Path
}
