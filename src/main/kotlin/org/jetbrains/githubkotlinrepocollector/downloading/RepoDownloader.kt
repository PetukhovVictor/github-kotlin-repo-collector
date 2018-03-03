package org.jetbrains.githubkotlinrepocollector.downloading

import com.sun.org.apache.xpath.internal.operations.Bool
import org.eclipse.jgit.api.Git
import org.jetbrains.githubkotlinjarcollector.collection.GithubAssetsCollector
import java.io.File

class RepoDownloader(private val reposDirectory: String) {
    val assetsCollector = GithubAssetsCollector(reposDirectory)

    fun downloadSource(repoName: String) {
        val repoDirectorySources = "$reposDirectory/$repoName/sources"
        val repoDirectorySourcesFile = File(repoDirectorySources)
        Git.cloneRepository()
                .setURI("https://github.com/$repoName.git")
                .setDirectory(repoDirectorySourcesFile)
                .call()

        println("SOURCES DOWNLOADED: $repoName")
    }

    fun downloadAssets(repoName: String): Boolean {
        val repo = assetsCollector.getRepository(repoName)

        return assetsCollector.assetsCollect(repo, "assets")
    }
}