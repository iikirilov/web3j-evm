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

    val receipts = HashMap<String, TransactionReceipt>()

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
        return evm.run(to, data, BigInteger.ZERO).result.output.toString()
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
            .getOrElse(ethSendTransaction.transactionHash) {
                throw RuntimeException("missing tx receipt")
            }
    }
}