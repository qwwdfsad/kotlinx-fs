package kotlinx.fs.core

import kotlin.test.*

class ListingTest : TestBase() {

    @Test
    fun testNonExistentListing() {
        val path = testDirectory("non-existent-listing")
        assertFailsWith<IOException> { path.list() }
        assertFailsWith<IOException> { path.walk() }
    }

    @Test
    fun testEmptyListing() {
        val directory = testDirectory("empty-listing").createDirectory()
        assertTrue(directory.list().isEmpty())
        assertTrue(directory.walk().isEmpty())
    }

    @Test
    fun testListing() {
        val directory = testDirectory("listing").createDirectory()
        Paths.getPath(directory.toString(), "1.txt").createFile()
        Paths.getPath(directory.toString(), "2").createDirectory()

        val expected = listOf(testFile("listing/1"), testDirectory("listing/2")).toSet()
        assertEquals(expected, directory.list().toSet())
        assertEquals(expected, directory.walk())
    }

    @Test
    fun testNestedListing() {
        val directory = testDirectory("nested-listing").createDirectory()
        Paths.getPath(directory.toString(), "1.txt").createFile()
        val nested = Paths.getPath(directory.toString(), "2").createDirectory()
        Paths.getPath(nested.toString(), "3.txt").createFile()

        val expected = listOf(testFile("nested-listing/1"), testDirectory("nested-listing/2")).toSet()
        assertEquals(expected, directory.list().toSet())
        assertEquals(expected, directory.walk())
    }

    private fun Path.walk(): Set<Path> {
        val result = mutableSetOf<Path>()
        walkDirectory {
           result.add(it)
        }
        return result
    }
}
