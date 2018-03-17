package org.jetbrains.githubkotlinrepocollector

import org.apache.commons.io.FileUtils
import org.jetbrains.githubkotlinjarcollector.collection.JarExtractor
import org.jetbrains.githubkotlinrepocollector.downloading.RepoDownloader
import org.jetbrains.githubkotlinrepocollector.filtering.RepoClassesFilter
import org.jetbrains.githubkotlinrepocollector.filtering.RepoSourcesFilter
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import java.io.File
import java.nio.file.Files

class RepoProcessor(private val reposDirectory: String) {
    companion object {
        private const val ASSETS_DIRECTORY = "assets"
        private const val JARS_DIRECTORY = "jars"
        private const val CLASSES_DIRECTORY = "classes"
        private const val SOURCES_DIRECTORY = "sources"
        private const val CST_DIRECTORY = "cst"
        private const val KOTLIN_COMPILER_PATH = "libs/kotlinc/bin/kotlinc"
    }

    private val repoDownloader = RepoDownloader(reposDirectory)
    private val repoSourcesFilter = RepoSourcesFilter(reposDirectory)
    private val repoClassesFilter = RepoClassesFilter(reposDirectory)

    private fun assetsProcess(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = "$reposDirectory/$repoName"
        val repoDirectoryAssets = "$repoDirectory/$ASSETS_DIRECTORY"
        val repoDirectoryJars = "$repoDirectoryAssets/$JARS_DIRECTORY"
        val classesDirectory = File("$repoDirectoryJars/$username/$repo/$CLASSES_DIRECTORY")

        DirectoryWalker(repoDirectoryAssets).run {
            JarExtractor(it, it.parentFile.name).extract()
            BytecodeRunner.walkAndParse(repoDirectoryJars, it.parentFile, username, repo, isPrint = false)
            it.delete()
        }
        if (Files.exists(classesDirectory.toPath())) {
            FileUtils.moveDirectory(classesDirectory, File("$repoDirectory/$CLASSES_DIRECTORY"))
        }
        File(repoDirectoryAssets).deleteRecursively()
    }

    private fun parsingToCst(username: String, repo: String, withCode: Boolean) {
        val repoName = "$username/$repo"
        val repoDirectory = "$reposDirectory/$repoName"

        val timeLogger = TimeLogger(task_name = "PARSING TO CST")
        CliRunner.run(KOTLIN_COMPILER_PATH, mapOf("" to "$repoDirectory/$SOURCES_DIRECTORY"), withPrint = false)
        timeLogger.finish()
    }

    private fun moveCstToCstFolder(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = "$reposDirectory/$repoName"
        val sourceDirectory = "$repoDirectory/$SOURCES_DIRECTORY"
        val cstDirectory = "$repoDirectory/$CST_DIRECTORY"

        DirectoryWalker(sourceDirectory).run {
            if (it.extension == "json") {
                val relativePath = it.relativeTo(File(sourceDirectory))
                val pathInCstFolder = File("$cstDirectory/$relativePath")
                File(pathInCstFolder.parent).mkdirs()
                Files.move(it.toPath(), pathInCstFolder.toPath())
            }
        }
    }

    fun downloadAndProcess(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = File("$reposDirectory/$repoName")

        repoDirectory.mkdirs()
        File("$reposDirectory/$repoName/cst").mkdirs()
        repoDownloader.downloadSource(repoName)
        repoSourcesFilter.filterByKtFiles("$repoName/$SOURCES_DIRECTORY")

        val isAssetsCollected = repoDownloader.downloadAssets(repoName)
        parsingToCst(username, repo, isAssetsCollected)
        moveCstToCstFolder(username, repo)

        if (isAssetsCollected) {
            assetsProcess(username, repo)
            repoClassesFilter.filter("$repoName/$CST_DIRECTORY", "$repoName/$CLASSES_DIRECTORY")
        }
        repoClassesFilter.removeCharsFromCst("$repoName/$CST_DIRECTORY")
    }
}