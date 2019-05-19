package org.web3j.evm

import org.web3j.crypto.Credentials
import tech.pegasys.pantheon.config.GenesisConfigFile

class EVMBuilder {
    lateinit var credentials: Credentials
    lateinit var genesisConfigFile: GenesisConfigFile

    fun credentials(credentials: Credentials) = apply { this.credentials = credentials }
    fun genesisConfigFile(genesisConfigFile: GenesisConfigFile) = apply { this.genesisConfigFile = genesisConfigFile }

    fun build() = EVM(credentials, genesisConfigFile)
}