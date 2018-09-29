package org.jetbrains.githubkotlinrepocollector.helpers

import java.util.*
import java.util.concurrent.TimeUnit


class TimeLogger {
    private val startTime = Date().time

    fun finish(task_name: String, fullFinish: Boolean = false) {
        val time = Date().time - startTime
        val seconds = TimeUnit.MILLISECONDS.toSeconds(time)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        val hours = TimeUnit.MILLISECONDS.toHours(time)

        if (fullFinish) {
            println("--------------------------------")
        }
        println("$task_name finished. Time: $hours:$minutes:$seconds")
        if (fullFinish) {
            println("--------------------------------")
        }
    }
}