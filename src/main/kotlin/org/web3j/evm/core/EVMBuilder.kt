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
package org.web3j.evm.core

import org.web3j.crypto.Credentials
import tech.pegasys.pantheon.config.GenesisConfigFile

class EVMBuilder {
    lateinit var credentials: Credentials
    lateinit var genesisConfigFile: GenesisConfigFile

    fun credentials(credentials: Credentials) =
            apply { this.credentials = credentials }

    fun genesisConfigFile(genesisConfigFile: GenesisConfigFile) =
            apply { this.genesisConfigFile = genesisConfigFile }

    fun build() = EVM(credentials, genesisConfigFile)
}
