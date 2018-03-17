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
        private const val PACKAGE_DIRECTIVE = "PACKAGE_DIRECTIVE"
    }

    private fun findPackageDirective(node: CstNode): String? {
        if (node.type == PACKAGE_DIRECTIVE) {
            return node.chars
        }

        if (node.children != null) {
            node.children!!.forEach {
                val packageDirective = findPackageDirective(it)

                if (packageDirective != null) {
                    return packageDirective
                }
            }
        }

        return null
    }

    private fun collectPackages(cstFolder: String): Set<String> {
        val cstNodeReference = object: TypeReference<ArrayList<CstNode>>() {}
        val packages = mutableSetOf<String>()

        JsonFilesReader<CstNode>("$reposDirectory/$cstFolder", JSON_EXT, cstNodeReference).run(true) { content: CstNode, file: File ->
            if (content.children != null) {
                val packageChars: String?

                if (content.children!![0].type == PACKAGE_DIRECTIVE) {
                    packageChars = content.children!![0].chars
                } else {
                    packageChars = findPackageDirective(content)
                }

                if (packageChars == null) {
                    println("NOT PACKAGE DIRECTIVE: '$file'")
                    return@run
                }

                val pattern = Pattern.compile("^package (?<package>.*?)$")
                val matcher = pattern.matcher(packageChars)
                if (matcher.matches()) {
                    packages.add(matcher.group("package"))
                } else {
                    packages.add("")
                    println("WITHOUT PACKAGE DETECTED: '$packageChars'")
                }
            } else {
                println("EMPTY FILE DETECTED: '$file'")
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

    fun removeCharsFromCst(cstFolder: String) {
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
    }
}