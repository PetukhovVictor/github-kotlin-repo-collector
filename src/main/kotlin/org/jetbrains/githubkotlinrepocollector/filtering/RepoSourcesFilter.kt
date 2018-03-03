package org.jetbrains.githubkotlinrepocollector.filtering

import org.jetbrains.githubkotlinrepocollector.io.DirectoryWalker
import java.io.File

class RepoSourcesFilter(private val reposDirectory: String) {
    companion object {
        fun deleteEmptyFolders(dir: String): Long {
            val f = File(dir)
            val listFiles = f.list()
            var totalSize: Long = 0
            for (file in listFiles!!) {
                val folder = File("$dir/$file")
                if (folder.isDirectory) {
                    totalSize += deleteEmptyFolders(folder.absolutePath)
                } else {
                    totalSize += folder.length()
                }
            }

            if (totalSize == 0L) {
                f.delete()
            }

            return totalSize
        }
    }

    fun filterByKtFiles(folder: String) {
        DirectoryWalker("$reposDirectory/$folder").run {
            if (it.extension != "kt") {
                it.delete()
            }
        }

        deleteEmptyFolders("$reposDirectory/$folder")
    }
}