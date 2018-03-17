package org.jetbrains.githubkotlinrepocollector

import com.fasterxml.jackson.core.type.TypeReference
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
import org.jetbrains.githubkotlinrepocollector.io.JsonFilesReader
import org.jetbrains.githubkotlinrepocollector.structures.RepoInfoList
import java.io.File
import java.nio.file.Files
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import org.jetbrains.bytecodeparser.Stage as BytecodeStage


object Runner {
    private const val JSON_EXT = "json"

    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoProcessor = RepoProcessor(reposDirectory)
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}
        val timeLoggerCommon = TimeLogger(task_name = "REPOS PROCESS")
        var reposTotal = 0
        var currentNumber = 0

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            reposTotal += content.items.size
        }

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            val timeLoggerChunk = TimeLogger(task_name = "REPOS CHUNK $file PROCESS")
            content.items.forEach repoLoop@{
                currentNumber++

                //  && File("$reposDirectory/${it.full_name}/cst").listFiles().isNotEmpty())
                if (Files.exists(File("$reposDirectory/${it.full_name}").toPath())
                        && Files.exists(File("$reposDirectory/${it.full_name}/cst").toPath())
                        && File("$reposDirectory/${it.full_name}/cst").listFiles().isNotEmpty()) {
                    println("SKIP REPO (ALREADY PROCESSED): '${it.full_name}'")
                    return@repoLoop
                }

                val timeLogger = TimeLogger(task_name = "REPO ${it.full_name} PROCESS ($currentNumber out of $reposTotal)")
                val repoIdentifier = it.full_name.split("/")

                repoProcessor.downloadAndProcess(username = repoIdentifier[0], repo = repoIdentifier[1])
                timeLogger.finish(fullFinish = true)
            }
            timeLoggerChunk.finish(fullFinish = true)
        }

        timeLoggerCommon.finish(fullFinish = true)
    }
}