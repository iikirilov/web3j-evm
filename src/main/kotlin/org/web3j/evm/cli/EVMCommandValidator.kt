package org.web3j.evm.cli

import com.google.common.io.Resources
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.reflect.KFunction1

fun validatePath(path: Path, fail: KFunction1<String, Nothing>) {
    try {
        Resources.toString(path.toUri().toURL(), StandardCharsets.UTF_8)
    } catch (e: FileNotFoundException) {
        fail("No such file $path")
    }
}