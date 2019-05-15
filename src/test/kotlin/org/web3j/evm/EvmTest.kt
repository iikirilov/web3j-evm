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

import com.google.common.collect.ImmutableSet
import org.junit.jupiter.api.Test
import tech.pegasys.pantheon.crypto.SECP256K1
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain
import tech.pegasys.pantheon.ethereum.core.Address
import tech.pegasys.pantheon.ethereum.core.Block
import tech.pegasys.pantheon.ethereum.core.BlockBody
import tech.pegasys.pantheon.ethereum.core.BlockHeader
import tech.pegasys.pantheon.ethereum.core.Hash
import tech.pegasys.pantheon.ethereum.core.LogsBloomFilter
import tech.pegasys.pantheon.ethereum.core.Transaction
import tech.pegasys.pantheon.ethereum.core.Wei
import tech.pegasys.pantheon.ethereum.debug.TraceOptions
import tech.pegasys.pantheon.ethereum.mainnet.ConstantinopleFixGasCalculator
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHeaderFunctions
import tech.pegasys.pantheon.ethereum.mainnet.MainnetContractCreationProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetEvmRegistries
import tech.pegasys.pantheon.ethereum.mainnet.MainnetMessageCallProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetPrecompiledContractRegistries
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSpecs.SPURIOUS_DRAGON_CONTRACT_SIZE_LIMIT
import tech.pegasys.pantheon.ethereum.mainnet.MainnetTransactionProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetTransactionValidator
import tech.pegasys.pantheon.ethereum.mainnet.PrecompiledContractConfiguration
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStorageWorldStateStorage
import tech.pegasys.pantheon.ethereum.trie.MerklePatriciaTrie
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup
import tech.pegasys.pantheon.ethereum.vm.DebugOperationTracer
import tech.pegasys.pantheon.ethereum.vm.MessageFrame.DEFAULT_MAX_STACK_SIZE
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage
import tech.pegasys.pantheon.util.bytes.BytesValue
import tech.pegasys.pantheon.util.uint.UInt256
import java.math.BigInteger
import java.util.Optional

class EvmTest {

    @Test
    fun transactionPassesTroughEvm() {
        val gasCalculator = ConstantinopleFixGasCalculator()
        val chainId = BigInteger.valueOf(2018)

        val transactionProcessor = MainnetTransactionProcessor(
            gasCalculator,
            MainnetTransactionValidator(
                gasCalculator,
                false,
                Optional.of(chainId)),
            MainnetContractCreationProcessor(
                gasCalculator,
                MainnetEvmRegistries.constantinople(gasCalculator),
                false,
                SPURIOUS_DRAGON_CONTRACT_SIZE_LIMIT,
                0),
            MainnetMessageCallProcessor(
                MainnetEvmRegistries.constantinople(gasCalculator),
                MainnetPrecompiledContractRegistries.byzantium(
                    PrecompiledContractConfiguration(gasCalculator, null)),
                ImmutableSet.of(
                    Address.fromHexString("0x0000000000000000000000000000000000000003")) //RIPEMD160_PRECOMPILE
            ),
            false,
            DEFAULT_MAX_STACK_SIZE)

        val inMemoryKeyValueStorage = InMemoryKeyValueStorage()
        val keyValueStorageWorldStateStorage =
            KeyValueStorageWorldStateStorage(inMemoryKeyValueStorage)
        val worldStateArchive = WorldStateArchive(keyValueStorageWorldStateStorage)
        val mutableWorldState = worldStateArchive.mutable
        val mutableWorldStateUpdater = mutableWorldState.updater()

        // FIXME() hack to make things work
        val account = mutableWorldStateUpdater
            .createAccount(Address.fromHexString("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"))
        account.incrementBalance(Wei.fromEth(100))
        mutableWorldStateUpdater.commit()

        val blockHeaderFunctions = MainnetBlockHeaderFunctions()
        val genesisBlock = Block(
            BlockHeader(
                Hash.fromHexString(
                    "0x0000000000000000000000000000000000000000000000000000000000000000"),
                Hash.wrap(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH),
                Address.fromHexString("0x0000000000000000000000000000000000000000"),
                Hash.wrap(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH),
                Hash.wrap(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH),
                Hash.wrap(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH),
                LogsBloomFilter(),
                UInt256.fromHexString("0x10000"),
                0L,
                UInt256.fromHexString("0x1fffffffffffff").toLong(),
                0L,
                0L,
                BytesValue.fromHexString(
                    "0x11bbe8db4e347b4e8c937c1c8370e4b5ed33adb3db69cbdb7a38e1e50b1b82fa"),
                Hash.fromHexString(
                    "0x0000000000000000000000000000000000000000000000000000000000000000"),
                UInt256.fromHexString("0x42").toLong(),
                blockHeaderFunctions
            ), BlockBody(emptyList(), emptyList()))

        val blockchain = DefaultMutableBlockchain(
            genesisBlock,
            KeyValueStoragePrefixedKeyBlockchainStorage(
                inMemoryKeyValueStorage,
                blockHeaderFunctions),
            NoOpMetricsSystem())

        val keypair = SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                BigInteger(
                    "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", 16
                )
            )
        )

        val transaction = Transaction.builder()
            .nonce(0L)
            .gasPrice(Wei.of(1000))
            .gasLimit(21000)
            .to(Address.fromHexString("0x627306090abaB3A6e1400e9345bC60c78a8BEf57"))
            .value(Wei.ZERO)
            .payload(BytesValue.EMPTY)
            .sender(Address.fromHexString("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"))
            .chainId(chainId)
            .signAndBuild(keypair)

        val result = transactionProcessor.processTransaction(
            blockchain,
            mutableWorldStateUpdater,
            genesisBlock.header,
            transaction,
            Address.fromHexString("0x0000000000000000000000000000000000000000"),
            DebugOperationTracer(TraceOptions.DEFAULT),
            BlockHashLookup(genesisBlock.header, blockchain),
            false)

        assert(result.isSuccessful)
    }
}
