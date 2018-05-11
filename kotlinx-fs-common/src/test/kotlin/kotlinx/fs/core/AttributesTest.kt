package kotlinx.fs.core

import kotlinx.fs.core.attributes.*
import kotlin.test.*

class AttributesTest : TestBase() {

    @Test
    fun testFileAttributes() {
        val file = testFile("file-attributes").createFile()
        val attributes = file.attributes()
        assertTrue(attributes.isFile)
        assertFalse(attributes.isDirectory)
        assertFalse(attributes.isSymbolicLink)
        assertEquals(0L, attributes.sizeBytes)

        checkTime(attributes.creationTimeUs)
        checkTime(attributes.lastAccessTimeUs)
        checkTime(attributes.lastModifiedTimeUs)

        file.writeBytes(ByteArray(42) {it.toByte()})
        val attributes2 = file.attributes()
        assertTrue(attributes2.sizeBytes >= 42) // FS-dependent
    }

    private fun checkTime(timestampUs: Long) {
        // Yay, common time module!
        val seconds = timestampUs / 1e6.toLong()
        // in now()..now() * 10 ~~ 2018/5/10..2453/07/23
        assertTrue(seconds in 1525968223..15259682219)
    }

    @Test
    fun testDirectoryAttributes() {
        val directory = testFile("directory-attributes").createDirectory()
        val attributes = directory.attributes()
        assertFalse(attributes.isFile)
        assertTrue(attributes.isDirectory)
        assertFalse(attributes.isSymbolicLink)
    }

    @Test
    fun testMissingAttributes() {
        val path = testFile("missing-attributes")
        assertFailsWith<IOException> { path.attributes() }
    }

    @Test
    fun testPosixPermissions() {
        val file = testFile("posix-permissions-file").createFile()
        assertEquals("0640", file.attributesOfType<PosixFileAttributes>().permissions.toOctString())

        val directory = testFile("posix-permissions-directory").createFile()
        assertEquals("0640", directory.attributesOfType<PosixFileAttributes>().permissions.toOctString())

        val root = getDefaultFileSystem().getPath("/")
        assertEquals("0750", root.attributesOfType<PosixFileAttributes>().permissions.toOctString())
    }
}
