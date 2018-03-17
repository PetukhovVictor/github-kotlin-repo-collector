package org.jetbrains.githubkotlinrepocollector

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object CliRunner {
    private fun getArgsStr(args: Map<String, Any>): String {
        if (args.size == 1 && args.contains("")) {
            return args[""].toString()
        }

        var argsStr = ""
        args.map {
            if (it.value !is Boolean || it.value == true) {
                argsStr += " -${it.key}"
            }
            if (it.value !is Boolean) {
                argsStr += " ${it.value}"
            }
        }
        return argsStr
    }

    private fun printResult(process: Process) {
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        var str = stdInput.readLine()

        while (str != null) {
            println(str)
            str = stdInput.readLine()
        }
    }

    fun run(command: String, args: Map<String, Any>, withPrint: Boolean = true) {
        val process = Runtime.getRuntime().exec("$command ${getArgsStr(args)}")
        process.waitFor(30, TimeUnit.SECONDS)
    }
}