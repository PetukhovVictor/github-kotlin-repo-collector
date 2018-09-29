package org.jetbrains.githubkotlinrepocollector.filtering

import java.io.File
import java.util.regex.Pattern

class RepoClassesFilter(private val reposDirectory: String) {
    companion object {
        private const val MULTILINE_COMMENT_REGEX = """/\*\*?.*\*/"""
        private const val SINGLELINE_COMMENT_REGEX = "//.*(?=\n)"
        private const val STRINGS_REGEX = "/\".*\"/"
        private const val MULTILINE_STRINGS_REGEX = "/\"\"\".*\"\"\"/"

        private val packagePattern = Pattern.compile("""(?:;|\n)\s*package\s+(?<package>.*?)\s*(?:;|\n)""")

        private fun collectPackages(sourcesFolder: String): Set<String> {
            val packages = mutableSetOf<String>()

            File(sourcesFolder).walkTopDown().forEach {
                if (it.extension != "kt") return@forEach

                val contentWithoutCommentsAndString = it.readText()
                        .replace(MULTILINE_COMMENT_REGEX.toRegex(RegexOption.DOT_MATCHES_ALL), "")
                        .replace(SINGLELINE_COMMENT_REGEX.toRegex(), "")
                        .replace(MULTILINE_STRINGS_REGEX.toRegex(RegexOption.DOT_MATCHES_ALL), "")
                        .replace(STRINGS_REGEX.toRegex(), "")

                val matcher = packagePattern.matcher(contentWithoutCommentsAndString)

                packages.add(if (matcher.find()) matcher.group("package") else "")
            }

            return packages
        }
    }

    fun filter(sourcesFolder: String, classesFolder: String) {
        val packages = collectPackages("$reposDirectory/$sourcesFolder")
        val classesDirectory = "$reposDirectory/$classesFolder"

        File(classesDirectory).walkTopDown().forEach {
            if (it.extension != "kt") return@forEach

            val `package` = it.relativeTo(File(classesDirectory)).parent.replace("/", ".")

            if (!packages.contains(`package`))
                it.delete()
        }
    }
}