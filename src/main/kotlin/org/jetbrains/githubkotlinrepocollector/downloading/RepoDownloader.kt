package org.jetbrains.githubkotlinrepocollector.downloading

import org.eclipse.jgit.api.Git
import org.jetbrains.githubkotlinjarcollector.collection.GithubAssetsCollector
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import java.io.File

class RepoDownloader(private val reposDirectory: String) {
    private val assetsCollector = GithubAssetsCollector(reposDirectory)

    fun downloadSource(repoName: String) {
        val repoDirectorySources = "$reposDirectory/$repoName/sources"
        val repoDirectorySourcesFile = File(repoDirectorySources)
        val timeLogger = TimeLogger(task_name = "DOWNLOAD SOURCES")

        Git.cloneRepository()
                .setURI("https://github.com/$repoName.git")
                .setDirectory(repoDirectorySourcesFile)
                .call()

        timeLogger.finish()
    }

    fun downloadAssets(repoName: String): Boolean {
        val repo = assetsCollector.getRepository(repoName)

        return assetsCollector.assetsCollect(repo, "assets")
    }
}