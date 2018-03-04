package org.jetbrains.githubkotlinrepocollector

import com.fasterxml.jackson.core.type.TypeReference
import org.jetbrains.githubkotlinrepocollector.helpers.TimeLogger
import org.jetbrains.githubkotlinrepocollector.io.JsonFilesReader
import org.jetbrains.githubkotlinrepocollector.structures.RepoInfoList
import java.io.File
import org.jetbrains.bytecodeparser.Runner as BytecodeRunner
import org.jetbrains.bytecodeparser.Stage as BytecodeStage


object Runner {
    private const val JSON_EXT = "json"

    fun run(repoInfoDirectory: String, reposDirectory: String) {
        val repoProcessor = RepoProcessor(reposDirectory)
        val repoInfoNodeReference = object: TypeReference<RepoInfoList>() {}
        val timeLoggerCommon = TimeLogger(task_name = "REPOS PROCESS")

        JsonFilesReader<RepoInfoList>(repoInfoDirectory, JSON_EXT, repoInfoNodeReference).run(true) { content: RepoInfoList, file: File ->
            val timeLoggerChunk = TimeLogger(task_name = "REPOS CHUNK $file PROCESS")
            content.items.forEach repoLoop@{
                val timeLogger = TimeLogger(task_name = "REPO ${it.full_name} PROCESS")
                val repoIdentifier = it.full_name.split("/")

                repoProcessor.downloadAndProcess(username = repoIdentifier[0], repo = repoIdentifier[1])
                timeLogger.finish(fullFinish = true)
            }
            timeLoggerChunk.finish(fullFinish = true)
        }

        timeLoggerCommon.finish(fullFinish = true)
    }
}