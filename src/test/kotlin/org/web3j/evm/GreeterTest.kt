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
package org.web3j.evm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import org.web3j.evm.core.EVM
import org.web3j.greeter.Greeter
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import tech.pegasys.pantheon.config.GenesisConfigFile

class GreeterTest {

    val PRIVATE_KEY = "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"

    @Test
    fun greeterDeploysInEvm() {
        val evm = EVM.builder()
            .credentials(Credentials.create(PRIVATE_KEY))
            .genesisConfigFile(GenesisConfigFile.development())
            .build()
        val web3j = Web3j.build(HttpService())
        val txManager = EVMTransactionManager(evm, web3j)

        val greeter = Greeter.deploy(web3j, txManager, DefaultGasProvider(), "Hello EVM").send()
        val greeting = greeter.greet().send()
        assertEquals("Hello EVM", greeting)
    }
}
