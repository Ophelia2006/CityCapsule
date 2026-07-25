package com.y.citycapsule.designsystem

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceArchitectureGuardTest {
    @Test
    fun placeFeatureUsesTypedRoutesStorageAndSharedTheme() {
        val projectRoot = generateSequence(
            File(requireNotNull(System.getProperty("user.dir"))).canonicalFile,
            File::getParentFile
        ).firstOrNull { File(it, "shared/src/commonMain").isDirectory }
            ?: error("Cannot locate CityCapsule project root.")
        val featureRoot = File(
            projectRoot,
            "shared/src/commonMain/kotlin/com/y/citycapsule/feature/place"
        )
        val forbidden = listOf(
            "MMKV",
            "openPage(",
            "\"place_list\"",
            "\"place_detail\"",
            "\"place_editor\"",
            "\"places.catalog\"",
            "\"favorites.place_ids\""
        )
        val violations = featureRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    forbidden.firstOrNull(line::contains)?.let {
                        "${file.name}:${index + 1} contains $it"
                    }
                }
            }
            .toList()

        assertTrue(
            "Place feature must depend on typed routes, repositories and AppTheme:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
