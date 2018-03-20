package org.jetbrains.githubkotlinrepocollector.downloading

import org.eclipse.jgit.api.Git
import org.jetbrains.githubkotlinjarcollector.collection.GithubAssetsCollector
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import org.kohsuke.github.GHFileNotFoundException
import java.io.File
import java.nio.file.Files

class RepoDownloader(private val reposDirectory: String) {
    private val assetsCollector = GithubAssetsCollector(reposDirectory)

    fun downloadSource(repoName: String): Boolean {
        val repoDirectorySources = "$reposDirectory/$repoName/sources"
        val repoDirectorySourcesFile = File(repoDirectorySources)
        val timeLogger = TimeLogger(task_name = "DOWNLOAD SOURCES")

        if (Files.exists(repoDirectorySourcesFile.toPath())) {
            repoDirectorySourcesFile.deleteRecursively()
        }

        try {
            Git.cloneRepository()
                    .setURI("https://github.com/$repoName.git")
                    .setDirectory(repoDirectorySourcesFile)
                    .call()

            timeLogger.finish()

            return true
        } catch (e: Exception) {
            println("DOWNLOAD SOURCES ERROR (maybe already removed): $e")

            timeLogger.finish()

            return false
        }
    }

    fun downloadAssets(repoName: String): Boolean {
        var assetsCollected: Boolean

        try {
            val repo = assetsCollector.getRepository(repoName)
            assetsCollected = assetsCollector.assetsCollect(repo, "assets")
        } catch (e: GHFileNotFoundException) {
            println("DOWNLOAD ASSETS ERROR (repo maybe already removed): $e")
            assetsCollected = false
        }

        return assetsCollected
    }
}