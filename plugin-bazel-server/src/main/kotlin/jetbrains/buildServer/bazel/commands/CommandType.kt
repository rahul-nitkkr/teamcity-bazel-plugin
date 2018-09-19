/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.bazel.commands

/**
 * Provides command-specific resources.
 */
interface CommandType {
    val name: String
    val editPage: String
    val viewPage: String
}