package org.jetbrains.githubkotlinrepocollector

import org.jetbrains.githubkotlinjarcollector.collection.JarExtractor
import org.jetbrains.githubkotlinrepocollector.downloading.RepoDownloader
import org.jetbrains.githubkotlinrepocollector.filtering.RepoSourcesFilter
import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
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

    private fun assetsProcess(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = "$reposDirectory/$repoName"
        val repoDirectoryAssets = "$repoDirectory/$ASSETS_DIRECTORY"
        val repoDirectoryJars = "$repoDirectoryAssets/$JARS_DIRECTORY"

        DirectoryWalker(repoDirectoryAssets).run {
            JarExtractor(it, it.parentFile.name).extract()
            org.jetbrains.bytecodeparser.Runner.walkAndParse(repoDirectoryJars, it.parentFile, username, repo)
            it.delete()
        }
        Files.move(File("$repoDirectoryJars/$username/$repo/$CLASSES_DIRECTORY").toPath(), File("$repoDirectory/$CLASSES_DIRECTORY").toPath())
        File(repoDirectoryAssets).deleteRecursively()
    }

    fun downloadAndProcess(username: String, repo: String) {
        val repoName = "$username/$repo"

        repoDownloader.downloadSource(repoName)
        repoSourcesFilter.filterByKtFiles("$repoName/$SOURCES_DIRECTORY")
        val isAssetsCollected = repoDownloader.downloadAssets(repoName)

        if (isAssetsCollected) {
            assetsProcess(username, repo)
        }
    }
}