package kotlinx.fs.core.internal

// Hack to have common implementation
interface PathCommon {
    fun getParent(): PathCommon?
    fun getFileName(): PathCommon?
    fun isAbsolute(): Boolean
    fun getNameCount(): Int
    fun getName(index: Int): PathCommon
    fun resolve(other: PathCommon): PathCommon
}

class UnixPath private constructor(path: String, normalizePath: Boolean) : PathCommon {
    constructor(path: String) : this(path, true)

    internal val normalizedPath: String = if (normalizePath) normalize(path) else path

    // Offsets of path parts. E.g. for /dir/folder.txt it will be [1, 5]
    private val partsOffsets: IntArray by lazy(mode = LazyThreadSafetyMode.NONE) { calculateOffsets() }

    private fun calculateOffsets(): IntArray {
        val result = IntArray(countNameParts(normalizedPath))
        var count = 0
        var index = 0

        val length = normalizedPath.length
        while (index < length) {
            val c = normalizedPath[index]
            if (c == '/') {
                index++
            } else {
                result[count++] = index++
                while (index < length && normalizedPath[index] != '/') {
                    index++
                }
            }
        }

        return result
    }

    private fun countNameParts(path: String): Int {
        if (normalizedPath.isEmpty()) {
            return 1
        }

        var currentIndex = 0
        var result = 0

        while (currentIndex < path.length) {
            if (path[currentIndex++] != '/') {
                ++result

                while (currentIndex < path.length && path[currentIndex] != '/') {
                    ++currentIndex
                }
            }
        }

        return result

    }

    override fun getParent(): PathCommon? {
        if (partsOffsets.isEmpty()) {
            return null
        }

        /*
         * /file.txt, last offset is 1, should return root
         * file.txt, last offset is 0, should return null
         */
        val end = partsOffsets.last() - 1
        if (end == 0) {
            return UnixPath("/", false)
        } else if (end == -1) {
            return null
        }

        return UnixPath(normalizedPath.substring(0, end), false)
    }


    override fun getFileName(): PathCommon? {
        if (partsOffsets.isEmpty()) return null
        return UnixPath(normalizedPath.substring(partsOffsets.last(), normalizedPath.length))
    }

    override fun isAbsolute(): Boolean = normalizedPath.startsWith('/')

    override fun getNameCount(): Int = partsOffsets.size

    override fun getName(index: Int): PathCommon {
        if (index < 0 || index >= partsOffsets.size) {
            throw IllegalArgumentException("Illegal index: $index, should be in [0, ${partsOffsets.size - 1}]")
        }

        val begin = partsOffsets[index]
        val len: Int
        len = if (index == partsOffsets.size - 1) {
            normalizedPath.length - begin
        } else {
            partsOffsets[index + 1] - begin - 1
        }

        // construct result

        return UnixPath(normalizedPath.substring(begin, begin + len), false)
    }

    override fun resolve(other: PathCommon): UnixPath {
        if (other !is UnixPath) {
            throw IllegalArgumentException("Mixing paths of different classes are forbidden," +
                    " target class: ${UnixPath::class}, received class: ${other::class}")
        }

        if (other.isAbsolute()) {
           return other
        }

        if (normalizedPath.isEmpty()) {
            return other
        }

        if (other.normalizedPath.isEmpty()) {
            return this
        }

        // TODO not the most optimal solution
        return UnixPath(normalizedPath + "/" + other.normalizedPath, true)
    }

    override fun toString(): String = normalizedPath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UnixPath

        if (normalizedPath != other.normalizedPath) return false

        return true
    }

    override fun hashCode(): Int {
        return normalizedPath.hashCode()
    }

    private fun normalize(path: String): String {
        val index = path.indexOf("//")
        if (index != -1) {
            return normalizeImpl(path, path.length, index) // index + 1?
        }

        if (path.isEmpty() || path[path.length - 1] != '/') {
            return path
        }

        return normalizeImpl(path, path.length, path.length - 1)
    }

    private fun normalizeImpl(input: String, length: Int, startFrom: Int): String {
        if (length == 0) return input

        // Trim trailing slashes
        var lastIndex = length
        while ((lastIndex > 0) && (input[lastIndex - 1] == '/')) lastIndex--
        if (lastIndex == 0) return "/"


        val sb = StringBuilder(input.length + (input.length - lastIndex))

        if (startFrom > 0) {
            sb.append(input, 0, startFrom)
        }

        var previousChar: Char = 0.toChar()
        for (i in startFrom until lastIndex) {
            val c = input[i]
            if (c == '/' && previousChar == '/') {
                continue
            }

            sb.append(c)
            previousChar = c
        }

        return sb.toString()
    }
}
