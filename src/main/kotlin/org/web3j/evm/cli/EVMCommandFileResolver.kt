package org.web3j.evm.cli

import com.google.common.io.Resources
import tech.pegasys.pantheon.config.GenesisConfigFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path

fun resolveGenesisConfig(genesis: Path?): GenesisConfigFile {
    return try {
        if (genesis != null) {
            GenesisConfigFile.fromConfig(Resources.toString(genesis.toUri().toURL(), StandardCharsets.UTF_8))
        } else {
            GenesisConfigFile.development()
        }
    } catch (e: Exception) {
        throw e
    }
}