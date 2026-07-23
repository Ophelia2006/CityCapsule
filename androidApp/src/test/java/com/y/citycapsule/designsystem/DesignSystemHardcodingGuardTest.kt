package com.y.citycapsule.designsystem

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemHardcodingGuardTest {
    @Test
    fun sharedBusinessUiDoesNotIntroduceRawVisualValues() {
        val projectRoot = findProjectRoot()
        val sourceRoot = File(
            projectRoot,
            "shared/src/commonMain/kotlin/com/y/citycapsule"
        )
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val relativePath = file.relativeTo(sourceRoot).invariantSeparatorsPath
                if (relativePath in ALLOWLIST) {
                    emptySequence()
                } else {
                    file.readLines().asSequence().mapIndexedNotNull { index, rawLine ->
                        val code = rawLine.substringBefore("//")
                        val rule = FORBIDDEN_RULES.firstOrNull { it.pattern.containsMatchIn(code) }
                        rule?.let { "$relativePath:${index + 1} [${it.name}] ${rawLine.trim()}" }
                    }
                }
            }
            .toList()

        assertTrue(
            "Raw visual values must live in design tokens or the documented diagnostic " +
                "allowlist:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun allowlistContainsOnlyTokenRegistriesAndTheImageDiagnostic() {
        assertEqualsStable(
            setOf(
                "designsystem/tokens/AppColors.kt",
                "designsystem/tokens/AppDimensions.kt",
                "designsystem/tokens/AppTypography.kt",
                "ImageAdapterBenchmarks.kt"
            ),
            ALLOWLIST
        )
    }

    private fun findProjectRoot(): File = generateSequence(
        File(System.getProperty("user.dir")).canonicalFile,
        File::getParentFile
    ).firstOrNull { File(it, "shared/src/commonMain").isDirectory }
        ?: error("Cannot locate CityCapsule project root from ${System.getProperty("user.dir")}")

    private fun assertEqualsStable(expected: Set<String>, actual: Set<String>) {
        assertTrue("Expected $expected but was $actual", expected == actual)
    }

    private data class Rule(val name: String, val pattern: Regex)

    private companion object {
        val ALLOWLIST = setOf(
            // Single sources of truth for production visual values.
            "designsystem/tokens/AppColors.kt",
            "designsystem/tokens/AppDimensions.kt",
            "designsystem/tokens/AppTypography.kt",
            // Fixed geometry and RGB overlays are assertions of the image-adapter benchmark.
            "ImageAdapterBenchmarks.kt"
        )

        val FORBIDDEN_RULES = listOf(
            Rule("hex-color", Regex("""Color\s*\(\s*0x[0-9A-Fa-f]{6,8}""")),
            Rule("css-color", Regex("""#[0-9A-Fa-f]{6,8}""")),
            Rule(
                "named-physical-color",
                Regex("""Color\.(Black|White|Red|Green|Blue|Yellow|Gray|DarkGray|LightGray|Magenta|Cyan)\b""")
            ),
            Rule(
                "raw-dimension",
                Regex("""(?<![\w.])(?:[1-9]\d*(?:\.\d+)?|0\.\d*[1-9]\d*)\.(dp|sp)\b""")
            )
        )
    }
}
