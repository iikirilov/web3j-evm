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

import org.web3j.evm.core.EVM
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.tx.TransactionManager
import org.web3j.tx.response.NoOpProcessor
import java.io.IOException
import java.lang.RuntimeException
import java.math.BigInteger

class EVMTransactionManager(val evm: EVM, web3j: Web3j) :
    TransactionManager(NoOpProcessor(web3j), "") {

    private val receipts = HashMap<String, TransactionReceipt>()

    override fun sendTransaction(
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        to: String?,
        data: String,
        value: BigInteger
    ): EthSendTransaction {
        val evmDump = evm.run(to, data, value)
        receipts.put(evmDump.transacitonReceipt.transactionHash, evmDump.transacitonReceipt)
        val response = EthSendTransaction()
        response.result = evmDump.transacitonReceipt.transactionHash
        return response
    }

    override fun sendCall(
        to: String,
        data: String,
        defaultBlockParameter: DefaultBlockParameter
    ): String {
        return evm.run(to, data, BigInteger.ZERO).output
    }

    @Throws(IOException::class, TransactionException::class)
    override fun executeTransaction(
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        to: String?,
        data: String,
        value: BigInteger
    ): TransactionReceipt {

        val ethSendTransaction = sendTransaction(
            gasPrice, gasLimit, to, data, value
        )
        return processResponse(ethSendTransaction)
    }

    private fun processResponse(ethSendTransaction: EthSendTransaction): TransactionReceipt {
        return receipts
            .computeIfPresent(ethSendTransaction.transactionHash) {
                    k, _ -> receipts.remove(k)
            } ?: throw RuntimeException("missing tx receipt")
    }
}
