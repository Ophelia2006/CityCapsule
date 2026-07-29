package com.y.citycapsule.navigation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellArchitectureGuardTest {

    @Test
    fun bottomNavigationHasExactlyOneProductOwner() {
        val sourceRoot = sharedSourceRoot()
        val owners = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot {
                it.relativeTo(sourceRoot).invariantSeparatorsPath ==
                    "designsystem/component/AppBottomNavigation.kt"
            }
            .filter { file ->
                file.readLines().any { line ->
                    line.trimStart().startsWith("AppBottomNavigation(")
                }
            }
            .map { it.relativeTo(sourceRoot).invariantSeparatorsPath }
            .toList()

        assertEquals(listOf("app/navigation/AppRootScaffold.kt"), owners)
    }

    @Test
    fun appShellSwitchesRetainedRootsWithoutRouteReplace() {
        val source = File(sharedSourceRoot(), "app/navigation/AppShellPage.kt").readText()

        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("pagerState.animateScrollToPage("))
        assertTrue(source.contains("userScrollEnabled = false"))
        assertFalse(source.contains("navigator.replace("))
        assertFalse(source.contains("navigator.navigate("))
    }

    @Test
    fun diagnosticsHaveNoProductNavigationReferences() {
        val sourceRoot = sharedSourceRoot()
        val allowed = setOf(
            "RouterPage.kt",
            "ImageAdapterBenchmarks.kt",
            "core/navigation/AppRouteTable.kt"
        )
        val forbiddenSymbols = listOf(
            "PAGE_ROUTER_DIAGNOSTICS",
            "PAGE_IMAGE_ADAPTER_DIAGNOSTICS"
        )
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { file -> file.relativeTo(sourceRoot).invariantSeparatorsPath to file }
            .filterNot { (path, _) -> path in allowed }
            .flatMap { (path, file) ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    forbiddenSymbols.firstOrNull(line::contains)
                        ?.let { symbol -> "$path:${index + 1} references $symbol" }
                }
            }
            .toList()

        assertTrue(
            "Diagnostics must not be reachable from product source:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    private fun sharedSourceRoot(): File {
        val projectRoot = generateSequence(
            File(System.getProperty("user.dir")).canonicalFile,
            File::getParentFile
        ).firstOrNull { File(it, "shared/src/commonMain").isDirectory }
            ?: error("Cannot locate CityCapsule project root from ${System.getProperty("user.dir")}")
        return File(projectRoot, "shared/src/commonMain/kotlin/com/y/citycapsule")
    }
}
