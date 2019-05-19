/*
 * Copyright 2019 WEB3 LABS LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.evm.cli

import com.google.common.io.Resources
import tech.pegasys.pantheon.config.GenesisConfigFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path

fun resolveGenesisConfig(genesis: Path?): GenesisConfigFile {
    return try {
        if (genesis != null) {
            GenesisConfigFile.fromConfig(
                Resources.toString(
                    genesis.toUri().toURL(),
                    StandardCharsets.UTF_8))
        } else {
            GenesisConfigFile.development()
        }
    } catch (e: Exception) {
        throw e
    }
}
