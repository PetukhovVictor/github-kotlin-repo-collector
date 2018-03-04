package org.jetbrains.githubkotlinrepocollector

import java.io.BufferedReader
import java.io.InputStreamReader

class PythonRunner {
    companion object {
        private const val interpreter = "python3"

        private fun getArgsStr(args: Map<String, Any>): String {
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

        fun run(libName: String, args: Map<String, Any>, withPrint: Boolean = true) {
            val process = Runtime.getRuntime().exec("$interpreter ./libs/$libName/main.py${getArgsStr(args)}")
            process.waitFor()
            if (withPrint) {
                printResult(process)
            }
        }
    }
}