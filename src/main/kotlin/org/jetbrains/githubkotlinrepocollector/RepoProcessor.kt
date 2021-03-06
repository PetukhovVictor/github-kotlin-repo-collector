package org.jetbrains.githubkotlinrepocollector

import org.apache.commons.io.FileUtils
import org.jetbrains.githubkotlinjarcollector.collection.JarExtractor
import org.jetbrains.githubkotlinrepocollector.downloading.RepoDownloader
import org.jetbrains.githubkotlinrepocollector.filtering.RepoClassesFilter
import org.jetbrains.githubkotlinrepocollector.filtering.RepoSourcesFilter
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
            val newClassesDirectory = File("$repoDirectory/$CLASSES_DIRECTORY")

            if (Files.exists(newClassesDirectory.toPath())) {
                newClassesDirectory.deleteRecursively()
            }

            FileUtils.moveDirectory(classesDirectory, newClassesDirectory)
        }
        File(repoDirectoryAssets).deleteRecursively()
    }

    fun downloadAndProcess(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = File("$reposDirectory/$repoName")

        repoDirectory.mkdirs()

        val isDownloaded = repoDownloader.downloadSource(repoName)

        if (!isDownloaded) return

        repoSourcesFilter.filterByKtFiles("$repoName/$SOURCES_DIRECTORY")

        val isAssetsCollected = repoDownloader.downloadAssets(repoName)

        if (isAssetsCollected) {
            assetsProcess(username, repo)
            repoClassesFilter.filter("$repoName/$SOURCES_DIRECTORY", "$repoName/$CLASSES_DIRECTORY")
        }
    }
}