package kotlinx.fs.core

import kotlin.test.*

class PathTest {

    @Test
    fun testEmptyPath() {
        val path = fs().getPath("")
        checkSingleFileName(path, "")
    }

    @Test
    fun testWorkingDirectory() {
        val path = fs().getPath(".")
        checkSingleFileName(path, ".")
    }

    @Test
    fun testParentDirectory() {
        val path = fs().getPath("..")
        checkSingleFileName(path, "..")
    }

    @Test
    fun testRoot() {
        val path = fs().getPath("/")
        assertTrue(path.isAbsolute())
        assertNull(path.getFileName())
        assertEquals(0, path.getNameCount())
        assertEquals("/", path.toString())
        assertNull(path.getParent())
        assertFailsWith<IllegalArgumentException> { path.getName(0) }
    }

    @Test
    fun testSingleFile() {
        val path = fs().getPath("42.txt")
        checkSingleFileName(path, "42.txt")
    }

    @Test
    fun testPathConcatenation() {
        // Where is my parametrized :(

        checkConcatenation(Path("a"), "b", "a/b")
        checkConcatenation(Path("a"), "/b/", "a/b")
        checkConcatenation(Path("foo/../.."), "../../", "foo/../../../..")
        checkConcatenation(Path(""), "/", "/")
        checkConcatenation(Path("/"), "", "/")
        checkConcatenation(Path("."), "bar", "./bar")
        checkConcatenation(Path("bar"), ".", "bar/.")
        checkConcatenation(Path("foo.txt"), "foo.txt", "foo.txt/foo.txt")
    }

    private fun checkConcatenation(base: Path, other: String, expected: String) {
        val result1 = base + other
        val result2 = base + Path(other)

        assertEquals(expected, result1.toString())
        assertEquals(expected, result2.toString())
    }

    private fun checkSingleFileName(path: Path, expectedName: String) {
        assertFalse(path.isAbsolute())
        assertEquals(expectedName, path.getFileName().toString())
        assertEquals(1, path.getNameCount())
        assertEquals(expectedName, path.toString())
        assertNull(path.getParent())
        assertFailsWith<IllegalArgumentException> { path.getName(1) }
    }

    @Test
    fun testRelativePathSlashes() {
        checkSlashes(fs().getPath("foo/bar//test///file.txt"))
        checkSlashes(fs().getPath("foo/bar//test///file.txt/"))
        checkSlashes(fs().getPath("foo/bar//test/file.txt//"))
    }

    @Test
    fun testAbsoluteRelativePathSlashes() {
        checkSlashes(fs().getPath("/foo/bar//test///file.txt"), "/")
        checkSlashes(fs().getPath("/foo/bar//test///file.txt/"), "/")
        checkSlashes(fs().getPath("/foo/bar//test/file.txt//"), "/")
    }

    private fun checkSlashes(path: Path, prefix: String = "") {
        assertEquals("file.txt", path.getFileName().toString())
        assertEquals(4, path.getNameCount())
        assertEquals(prefix + "foo/bar/test/file.txt", path.toString())
        assertEquals(prefix + "foo/bar/test", path.getParent()!!.toString())

        assertEquals("foo", path.getName(0).toString())
        assertEquals("bar", path.getName(1).toString())
        assertEquals("test", path.getName(2).toString())
        assertEquals("file.txt", path.getName(3).toString())
    }


    @Test
    fun testDenormalizedPath() {
        val path = fs().getPath("1/2/3/../3/../../2//1.txt")
        assertEquals("1.txt", path.getFileName().toString())
        assertEquals(9, path.getNameCount())
        assertEquals("1/2/3/../3/../../2/1.txt", path.toString())
        assertEquals("1/2/3/../3/../../2", path.getParent()!!.toString())

        assertEquals("1", path.getName(0).toString())
        assertEquals("2", path.getName(1).toString())
        assertEquals("3", path.getName(2).toString())
        assertEquals("..", path.getName(3).toString())
    }

    private fun fs() = getDefaultFileSystem()
}
