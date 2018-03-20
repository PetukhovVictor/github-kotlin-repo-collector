package org.jetbrains.githubkotlinrepocollector

import com.fasterxml.jackson.core.type.TypeReference
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import org.jetbrains.githubkotlinrepocollector.io.JsonFilesReader
import org.jetbrains.githubkotlinrepocollector.structures.RepoInfoList
import java.io.File
import java.nio.file.Files
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import org.jetbrains.bytecodeparser.Stage as BytecodeStage


object Runner {
    private const val JSON_EXT = "json"

    private fun countFiles(path: String): Int {
        var number = 0

        File(path).walkTopDown().forEach {
            if (it.isFile) {
                number++
            }
        }

        return number
    }

    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoProcessor = RepoProcessor(reposDirectory)
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}
        val timeLoggerCommon = TimeLogger(task_name = "REPOS PROCESS")
        var reposTotal = 0
        var currentNumber = 0

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            reposTotal += content.items.size
        }

        var removedRepo = 0
        var filesNumber = 0
        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            val timeLoggerChunk = TimeLogger(task_name = "REPOS CHUNK $file PROCESS")
            content.items.forEach repoLoop@{
                val cstFiles = countFiles("$reposDirectory/${it.full_name}/cst")
                val sourcesFiles = countFiles("$reposDirectory/${it.full_name}/sources")

                filesNumber += sourcesFiles

                currentNumber++

                if (Files.exists(File("$reposDirectory/${it.full_name}").toPath()) && (sourcesFiles == 0 || cstFiles == sourcesFiles)) {
                    println("SKIP REPO (ALREADY PROCESSED): '${it.full_name}'")
                    if (sourcesFiles == 0) {
                        removedRepo++
                    }
                    return@repoLoop
                }

                val timeLogger = TimeLogger(task_name = "REPO ${it.full_name} PROCESS ($currentNumber out of $reposTotal)")
                val repoIdentifier = it.full_name.split("/")

                repoProcessor.downloadAndProcess(username = repoIdentifier[0], repo = repoIdentifier[1])
                timeLogger.finish(fullFinish = true)
            }
            timeLoggerChunk.finish(fullFinish = true)
        }

        println("Removed repos: $removedRepo, total files: $filesNumber")

        timeLoggerCommon.finish(fullFinish = true)
    }
}