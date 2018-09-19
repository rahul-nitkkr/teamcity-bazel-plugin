/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.bazel

import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.discovery.BuildRunnerDiscoveryExtension
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject
import jetbrains.buildServer.util.browser.Browser
import jetbrains.buildServer.util.browser.Element
import kotlin.coroutines.experimental.buildSequence

/**
 * Performs bazel build steps discovery.
 */
class BazelRunnerDiscoveryExtension : BuildRunnerDiscoveryExtension {

    private val depthLimit = 3

    override fun discover(settings: BuildTypeSettings, browser: Browser): MutableList<DiscoveredObject> {
        return discoverRunners(browser.root, 0, null).toMutableList()
    }

    private fun discoverRunners(currentElement: Element, currentElementDepth: Int, workspaceDir: String?)
            : Sequence<DiscoveredObject> = buildSequence {
        if (currentElementDepth > depthLimit || currentElement.name.contains("rule")) {
            return@buildSequence
        }

        val children = (currentElement.children ?: emptyList())
        val workingDir = workspaceDir ?: if (children.any { BazelConstants.WORKSPACE_FILE_NAME == it.name }) {
            currentElement.fullName
        } else {
            null
        }

        if (workingDir != null) {
            discoverRunners(workingDir, currentElement.fullName, children.filter { it.isLeaf }).let { objects ->
                val foundObjects = objects.any()
                yieldAll(objects)

                // If we already found build steps in the directory don't go deeper
                if (foundObjects) {
                    return@buildSequence
                }
            }
        }

        // Scan nested directories
        children.forEach {
            if (!it.isLeaf) {
                yieldAll(discoverRunners(it, currentElementDepth + 1, workingDir))
            }
        }
    }

    private fun discoverRunners(workingDir: String, currentDir: String, files: List<Element>)
            : Sequence<DiscoveredObject> = buildSequence {
        files.forEach { item ->
            if (BazelConstants.BUILD_FILE_NAME.matches(item.name) && item.isContentAvailable) {
                val target = "//" + currentDir.substring(workingDir.length).trim('/') + "..."
                yield(DiscoveredObject(BazelConstants.RUNNER_TYPE, mapOf(
                        BazelConstants.PARAM_WORKING_DIR to workingDir,
                        BazelConstants.PARAM_COMMAND to BazelConstants.COMMAND_BUILD,
                        BazelConstants.PARAM_BUILD_TARGET to target
                )))
            }
        }
    }
}
