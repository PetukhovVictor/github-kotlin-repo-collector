package org.jetbrains.githubkotlinrepocollector

import com.xenomachina.argparser.ArgParser

fun main(args : Array<String>) {
    val parser = ArgParser(args)
    val repoInfoDirectory by parser.storing("-i", "--input", help="path to folder with repo info JSON files")
    val outputDirectory by parser.storing("-o", "--output", help="path to folder, in which will be written source codes and assets")


    Runner.run(repoInfoDirectory, outputDirectory)
}