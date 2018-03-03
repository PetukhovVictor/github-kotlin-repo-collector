package org.jetbrains.githubkotlinrepocollector

import com.fasterxml.jackson.core.type.TypeReference
import org.jetbrains.githubkotlinjarcollector.collection.JarExtractor
import org.jetbrains.githubkotlinrepocollector.downloading.RepoDownloader
import org.jetbrains.githubkotlinrepocollector.filtering.RepoSourcesFilter
import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
import org.jetbrains.githubkotlinrepocollector.io.JsonFilesReader
import org.jetbrains.githubkotlinrepocollector.structures.RepoInfoList
import java.io.File
import java.nio.file.Files
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import org.jetbrains.bytecodeparser.Stage as BytecodeStage


object Runner {
    private const val ASSETS_DIRECTORY = "assets"
    private const val JARS_DIRECTORY = "jars"
    private const val CLASSES_DIRECTORY = "classes"
    private const val SOURCES_DIRECTORY = "sources"
    private const val JSON_EXT = "json"

    fun assetsProcess(reposDirectory: String, username: String, repo: String) {
        val repoName = "$username/$repo"
        val repoDirectory = "$reposDirectory/$repoName"
        val repoDirectoryAssets = "$repoDirectory/$ASSETS_DIRECTORY"
        val repoDirectoryJars = "$repoDirectoryAssets/$JARS_DIRECTORY"

        DirectoryWalker(repoDirectoryAssets).run {
            JarExtractor(it, it.parentFile.name).extract()
            BytecodeRunner.walkAndParse(repoDirectoryJars, it.parentFile, username, repo)
            it.delete()
        }
        Files.move(File("$repoDirectoryJars/$username/$repo/$CLASSES_DIRECTORY").toPath(), File("$repoDirectory/$CLASSES_DIRECTORY").toPath())
        File(repoDirectoryAssets).deleteRecursively()
    }

    fun repoDownloadAndProcess(reposDirectory: String, username: String, repo: String) {
        val repoDownloader = RepoDownloader(reposDirectory)
        val repoSourcesFilter = RepoSourcesFilter(reposDirectory)
        val repoName = "$username/$repo"

        repoDownloader.downloadSource(repoName)
        repoSourcesFilter.filterByKtFiles("$repoName/$SOURCES_DIRECTORY")
        val assetsCollected = repoDownloader.downloadAssets(repoName)

        if (assetsCollected) {
            assetsProcess(reposDirectory, username, repo)
        }
    }

    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            content.items.forEach {
                val repoIdentifier = it.full_name.split("/")
                repoDownloadAndProcess(reposDirectory, username = repoIdentifier[0], repo = repoIdentifier[1])
            }
        }
    }
}