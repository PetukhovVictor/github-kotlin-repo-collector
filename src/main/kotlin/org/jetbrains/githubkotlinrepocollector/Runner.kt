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
    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, "json", repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            content.items.forEach itemsLoop@{
                val repoIdentifier = it.full_name.split("/")
                val repoDownloader = RepoDownloader(reposDirectory)
                val repoSourcesFilter = RepoSourcesFilter(reposDirectory)
                val username = repoIdentifier[0]
                val repo = repoIdentifier[1]
                val repoName = "$username/$repo"
                val repoDirectory = "$reposDirectory/$repoName"
                val repoDirectoryAssets = "$repoDirectory/assets"
                val repoDirectoryJars = "$repoDirectoryAssets/jars"

                repoDownloader.downloadSource(repoName)
                repoSourcesFilter.filterByKtFiles("$repoName/sources")
                val assetsCollected = repoDownloader.downloadAssets(repoName)

                if (assetsCollected) {
                    DirectoryWalker(repoDirectoryAssets).run {
                        JarExtractor(it, it.parentFile.name).extract()
                        BytecodeRunner.walkAndParse(repoDirectoryJars, it.parentFile, username, repo)
                        it.delete()
                    }
                    Files.move(File("$repoDirectoryJars/$username/$repo/classes").toPath(), File("$repoDirectory/classes").toPath())
                    File(repoDirectoryAssets).deleteRecursively()
                }
            }
        }
    }
}