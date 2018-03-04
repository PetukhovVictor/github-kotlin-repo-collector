package org.jetbrains.githubkotlinrepocollector.structures

abstract class AbstractCstNode {
    var type: String = ""
    abstract val children: List<AbstractCstNode>?
}