package org.jetbrains.githubkotlinrepocollector

import org.jetbrains.githubkotlinrepocollector.downloading.RepoDownloader
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import java.io.File

class RepoProcessor(private val reposDirectory: String) {
    private val repoDownloader = RepoDownloader(reposDirectory)

    fun downloadAndProcess(username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = File("$reposDirectory/$repoName")

        repoDirectory.mkdirs()
        repoDownloader.downloadSource(repoName)
    }
}