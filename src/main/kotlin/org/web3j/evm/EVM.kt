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
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.core.methods.response.TransactionReceipt
import tech.pegasys.pantheon.crypto.SECP256K1
import tech.pegasys.pantheon.ethereum.chain.Blockchain
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain
import tech.pegasys.pantheon.ethereum.core.Address
import tech.pegasys.pantheon.ethereum.core.Block
import tech.pegasys.pantheon.ethereum.core.BlockBody
import tech.pegasys.pantheon.ethereum.core.BlockHeader
import tech.pegasys.pantheon.ethereum.core.Hash
import tech.pegasys.pantheon.ethereum.core.LogsBloomFilter
import tech.pegasys.pantheon.ethereum.core.MutableWorldState
import tech.pegasys.pantheon.ethereum.core.Transaction
import tech.pegasys.pantheon.ethereum.core.Wei
import tech.pegasys.pantheon.ethereum.core.WorldUpdater
import tech.pegasys.pantheon.ethereum.debug.TraceOptions
import tech.pegasys.pantheon.ethereum.mainnet.ConstantinopleFixGasCalculator
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHeaderFunctions
import tech.pegasys.pantheon.ethereum.mainnet.MainnetContractCreationProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetEvmRegistries
import tech.pegasys.pantheon.ethereum.mainnet.MainnetMessageCallProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetPrecompiledContractRegistries
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSpecs
import tech.pegasys.pantheon.ethereum.mainnet.MainnetTransactionProcessor
import tech.pegasys.pantheon.ethereum.mainnet.MainnetTransactionValidator
import tech.pegasys.pantheon.ethereum.mainnet.PrecompiledContractConfiguration
import tech.pegasys.pantheon.ethereum.mainnet.TransactionProcessor
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStorageWorldStateStorage
import tech.pegasys.pantheon.ethereum.trie.MerklePatriciaTrie
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup
import tech.pegasys.pantheon.ethereum.vm.DebugOperationTracer
import tech.pegasys.pantheon.ethereum.vm.MessageFrame
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage
import tech.pegasys.pantheon.util.bytes.BytesValue
import tech.pegasys.pantheon.util.uint.UInt256
import java.lang.RuntimeException
import java.math.BigInteger
import java.util.Optional

class EVM(val credentials: Credentials) {

    val GAS_PRICE: BigInteger = BigInteger.ONE
    val GAS_LIMIT: Long = 30000000
    val CHAIN_ID: BigInteger = BigInteger.ONE

    val transactionProcessor: MainnetTransactionProcessor
    val blockchain: Blockchain
    val mutableWorldState: MutableWorldState
    val mutableWorldStateUpdater: WorldUpdater
    val genesisBlock: Block

    init {
        val gasCalculator = ConstantinopleFixGasCalculator()
        transactionProcessor = MainnetTransactionProcessor(
            gasCalculator,
            MainnetTransactionValidator(
                gasCalculator,
                false,
                Optional.of(CHAIN_ID)),
            MainnetContractCreationProcessor(
                gasCalculator,
                MainnetEvmRegistries.constantinople(gasCalculator),
                false,
                MainnetProtocolSpecs.SPURIOUS_DRAGON_CONTRACT_SIZE_LIMIT,
                0),
            MainnetMessageCallProcessor(
                MainnetEvmRegistries.constantinople(gasCalculator),
                MainnetPrecompiledContractRegistries.byzantium(
                    PrecompiledContractConfiguration(gasCalculator, null)
                ),
                ImmutableSet.of(
                    Address.fromHexString("0x0000000000000000000000000000000000000003")) //RIPEMD160_PRECOMPILE
            ),
            false,
            MessageFrame.DEFAULT_MAX_STACK_SIZE
        )
        val blockHeaderFunctions = MainnetBlockHeaderFunctions()
        genesisBlock = Block(
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
            ), BlockBody(emptyList(), emptyList())
        )
        val inMemoryKeyValueStorage = InMemoryKeyValueStorage()
        val keyValueStorageWorldStateStorage =
            KeyValueStorageWorldStateStorage(inMemoryKeyValueStorage)
        val worldStateArchive = WorldStateArchive(keyValueStorageWorldStateStorage)
        mutableWorldState = worldStateArchive.mutable
        mutableWorldStateUpdater = mutableWorldState.updater()

        // Give ether to user
        val account = mutableWorldStateUpdater
            .createAccount(Address.fromHexString(credentials.address))
        account.incrementBalance(Wei.fromEth(100))
        mutableWorldStateUpdater.commit()

        blockchain = DefaultMutableBlockchain(
            genesisBlock,
            KeyValueStoragePrefixedKeyBlockchainStorage(
                inMemoryKeyValueStorage,
                blockHeaderFunctions),
            NoOpMetricsSystem())
    }

    /**
     * Runs a transaction through the EVM
     *
     * Returns a transaction receipt
     */
    fun run(
        to: String?,
        data: String?,
        value: BigInteger?
    ): TransactionReceipt {
        val transaction = Transaction.builder()
            .nonce(getNonce())
            .gasPrice(Wei.of(GAS_PRICE))
            .gasLimit(GAS_LIMIT)
            .to(Address.fromHexString(to))
            .value(Wei.of(value))
            .payload(BytesValue.fromHexString(data))
            .sender(Address.fromHexString(credentials.address))
            .chainId(CHAIN_ID)
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        credentials.ecKeyPair.privateKey)))

        val worldUpdater = mutableWorldState.updater()
        val result = transactionProcessor.processTransaction(
            blockchain,
            worldUpdater,
            genesisBlock.header,
            transaction,
            Address.fromHexString("0x0000000000000000000000000000000000000000"),
            DebugOperationTracer(TraceOptions.DEFAULT),
            BlockHashLookup(genesisBlock.header, blockchain),
            false)

        if (!result.isInvalid) {
            worldUpdater.commit()
            mutableWorldState.persist()
            val logs = result.logs.map {
                Log(
                    false,
                    "",
                    "",
                    transaction.hash().toString(),
                    "",
                    "",
                    transaction.sender.toString(),
                    it.data.toString(),
                    "",
                    it.topics.map { topic -> topic.toString() })
            }
            return TransactionReceipt(
                transaction.hash().toString(), "", "", "",
                "" + (GAS_LIMIT - result.gasRemaining),
                "" + (GAS_LIMIT - result.gasRemaining),
                transaction.to.get().toString(),
                mutableWorldState.rootHash().toString(),
                convertStatus(result.status),
                transaction.sender.toString(),
                transaction.to.get().toString(),
                logs, "")
        } else {
            throw RuntimeException(result.validationResult.errorMessage)
        }
    }

    private fun getNonce(): Long {
        return mutableWorldState.get(Address.fromHexString(credentials.address)).nonce
    }

    private fun convertStatus(status: TransactionProcessor.Result.Status): String {
        return when (status) {
            TransactionProcessor.Result.Status.INVALID -> TODO()
            TransactionProcessor.Result.Status.SUCCESSFUL -> "0x1"
            TransactionProcessor.Result.Status.FAILED -> "0x0"
            else -> TODO()
        }
    }
}
