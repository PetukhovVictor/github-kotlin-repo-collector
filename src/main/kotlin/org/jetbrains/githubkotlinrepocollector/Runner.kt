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

    private fun countFiles(path: String) = File(path).walkTopDown().count { it.isFile }

    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoProcessor = RepoProcessor(reposDirectory)
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}
        var currentNumber = 0
        var reposTotal = 0

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content, _ ->
            reposTotal += content.items.size
        }

        val timeLoggerCommon = TimeLogger()
        var missedRepo = 0
        var filesNumber = 0

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content, file ->
            val timeLoggerChunk = TimeLogger()
            content.items.forEach repoLoop@{
                currentNumber++

                if (Files.exists(File("$reposDirectory/${it.full_name}").toPath())) {
                    println("SKIP REPO (ALREADY PROCESSED): '${it.full_name}'")
                    missedRepo++
                    return@repoLoop
                }

                val timeLogger = TimeLogger()
                val repoIdentifier = it.full_name.split("/")

                repoProcessor.downloadAndProcess(username = repoIdentifier[0], repo = repoIdentifier[1])

                val sourcesFiles = countFiles("$reposDirectory/${it.full_name}/sources")

                filesNumber += sourcesFiles
                timeLogger.finish(task_name = "REPO ${it.full_name} PROCESSING ($currentNumber out of $reposTotal, source files: $sourcesFiles)")
            }
            timeLoggerChunk.finish(task_name = "REPO LIST CHUNK $file PROCESSING", fullFinish = true)
        }

        println("Missed repos: $missedRepo, total files: $filesNumber")

        timeLoggerCommon.finish(task_name = "REPO LIST PROCESSING (total $reposTotal)", fullFinish = true)
    }
}