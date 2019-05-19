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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.evm.EVM
import org.web3j.evm.http.EVMService
import java.math.BigInteger
import java.nio.file.Path

class EVMCommand(args: Array<String>) : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    private val DEFAULT_KEY =
        BigInteger(
            "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
            16)

    val genesis: Path?
            by option(help = "genesis file config")
                .path()
                .validate { validatePath(it, this::fail) }

    val signingKey: BigInteger
            by option(help = "ethereum private key for signing")
                .convert { BigInteger(it) }
                .default(DEFAULT_KEY, DEFAULT_KEY.toString())

    init {
        main(args)
    }

    override fun run() {

        logger.info { "starting web3j-evm" }

        val genesisConfigFile = resolveGenesisConfig(genesis)

        val evm =
            EVM.builder()
            .credentials(Credentials.create(ECKeyPair.create(signingKey)))
            .genesisConfigFile(genesisConfigFile)
                .build()

        EVMService(evm)
    }
}

fun main(args: Array<String>) {
    EVMCommand(args)
}
