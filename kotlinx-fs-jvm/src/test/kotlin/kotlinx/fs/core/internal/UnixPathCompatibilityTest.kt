package kotlinx.fs.core.internal

import kotlinx.fs.core.*
import org.junit.Test
import org.junit.runner.*
import org.junit.runners.*
import kotlin.test.*

@RunWith(Parameterized::class)
class UnixPathCompatibilityTest(val path: String) {

    companion object {
        private val paths = listOf(
            "file.txt",
            "folder/file.txt",
            "1/2/3/file",
            "1//2///3/////4",
            "1/2/../2/",
            "",
            "/",
            "..",
            "."
        )

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun params(): Collection<Array<Any>> =
            paths.flatMap { listOf(it, "/$it", "$it/", "/$it/", "///$it//") }.toSet().map { arrayOf<Any>(it) }
    }

    @Test
    fun testPath() {
        checkCompatibility(path)
    }

    private fun checkCompatibility(path: String) {
        val unixPath = UnixPath(path)
        val javaPath = Paths.getPath(path)

        checkCompatibility(javaPath, unixPath)

        paths.forEach {
            val target = UnixPath(it)
            val javaTarget = Paths.getPath(it)
            val resolvedUnix = unixPath.resolve(target)
            val resolvedJava = javaPath.resolve(javaTarget)
            checkCompatibility(resolvedJava, resolvedUnix)
        }
    }

    private fun checkCompatibility(javaPath: Path, unixPath: UnixPath) {
        assertEquals(javaPath.toString(), unixPath.toString())
        assertEquals(javaPath.fileName?.toString(), unixPath.getFileName()?.toString())
        assertEquals(javaPath.parent?.toString(), unixPath.getParent()?.toString())
        assertEquals(javaPath.isAbsolute, unixPath.isAbsolute())

        val names = javaPath.nameCount
        assertEquals(names, unixPath.getNameCount())
        (0 until names).forEach {
            assertEquals(javaPath.getName(it).toString(), unixPath.getName(it).toString())
        }

        assertFailsWith<IllegalArgumentException> { javaPath.getName(names) }
        assertFailsWith<IllegalArgumentException> { unixPath.getName(names) }
    }
}
