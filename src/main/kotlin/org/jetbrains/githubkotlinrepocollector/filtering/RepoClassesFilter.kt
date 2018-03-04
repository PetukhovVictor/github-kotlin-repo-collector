package org.jetbrains.githubkotlinrepocollector.filtering

import com.fasterxml.jackson.core.type.TypeReference
import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
import org.jetbrains.githubkotlinrepocollector.io.FileWriter
import org.jetbrains.githubkotlinrepocollector.io.JsonFilesReader
import org.jetbrains.githubkotlinrepocollector.structures.CstNode
import org.jetbrains.githubkotlinrepocollector.structures.CstNodeMinified
import java.io.File
import java.nio.file.Files
import java.util.regex.Pattern

class RepoClassesFilter(private val reposDirectory: String) {
    companion object {
        private const val JSON_EXT = "json"
        private const val TEMP_CLASSES_DIRECTORY = "classes_tmp"
    }

    private fun collectPackages(cstFolder: String): Set<String> {
        val cstNodeReference = object: TypeReference<ArrayList<CstNode>>() {}
        val packages = mutableSetOf<String>()

        JsonFilesReader<CstNode>("$reposDirectory/$cstFolder", JSON_EXT, cstNodeReference).run { content: CstNode, file: File ->
            if (content.children != null) {
                if (content.children!![0].type == "PACKAGE_DIRECTIVE") {
                    val packageChars = content.children!![0].chars
                    val pattern = Pattern.compile("^package (?<package>.*?)$")
                    val matcher = pattern.matcher(packageChars)
                    if (matcher.matches()) {
                        packages.add(matcher.group("package"))
                    } else {
                        packages.add("")
                        println("NOT PACKAGE FOUND: '$packageChars'")
                    }
                } else {
                    println("PACKAGE_DIRECTIVE IS NOT FIRST!")
                }
            }
        }

        return packages
    }

    private fun walkCstAndRewriteWithoutChars(content: CstNode, node: CstNodeMinified) {
        node.type = content.type

        if (content.children != null) {
            node.children = mutableListOf()
            content.children!!.forEach {
                val newCstNode = CstNodeMinified()
                walkCstAndRewriteWithoutChars(it, newCstNode)
                node.children!!.add(newCstNode)
            }
        }
    }

    private fun removeCharsFromCst(cstFolder: String) {
        val cstNodeReference = object: TypeReference<ArrayList<CstNode>>() {}

        JsonFilesReader<CstNode>("$reposDirectory/$cstFolder", JSON_EXT, cstNodeReference).run { content: CstNode, file: File ->
            val cstWithoutChars = CstNodeMinified()
            walkCstAndRewriteWithoutChars(content, cstWithoutChars)
            FileWriter.write(file, cstWithoutChars)
        }
    }

    fun filter(cstFolder: String, classesFolder: String) {
        val packages = collectPackages(cstFolder)
        val classesDirectory = "$reposDirectory/$classesFolder"
        val classesTempDirectory = File("$reposDirectory/${File(classesFolder).parent}/$TEMP_CLASSES_DIRECTORY")

        packages.forEach {
            val packagePath = it.split(".").joinToString("/")
            val packageDirectory = "$classesTempDirectory/$packagePath"

            File(packageDirectory).mkdirs()
            DirectoryWalker("$classesDirectory/$packagePath", maxDepth = 1).run {
                Files.copy(it.toPath(), File("$packageDirectory/${it.name}").toPath())
            }
        }

        File(classesDirectory).deleteRecursively()
        classesTempDirectory.renameTo(File(classesDirectory))
        removeCharsFromCst(cstFolder)
    }
}