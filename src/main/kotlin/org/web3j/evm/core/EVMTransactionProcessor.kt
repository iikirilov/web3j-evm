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

import mu.KotlinLogging
import tech.pegasys.pantheon.ethereum.chain.Blockchain
import tech.pegasys.pantheon.ethereum.core.Address
import tech.pegasys.pantheon.ethereum.core.Gas
import tech.pegasys.pantheon.ethereum.core.LogSeries
import tech.pegasys.pantheon.ethereum.core.ProcessableBlockHeader
import tech.pegasys.pantheon.ethereum.core.Transaction
import tech.pegasys.pantheon.ethereum.core.WorldUpdater
import tech.pegasys.pantheon.ethereum.mainnet.AbstractMessageProcessor
import tech.pegasys.pantheon.ethereum.mainnet.TransactionProcessor.Result
import tech.pegasys.pantheon.ethereum.mainnet.TransactionProcessor.Result.Status
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason
import tech.pegasys.pantheon.ethereum.mainnet.ValidationResult
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup
import tech.pegasys.pantheon.ethereum.vm.Code
import tech.pegasys.pantheon.ethereum.vm.GasCalculator
import tech.pegasys.pantheon.ethereum.vm.MessageFrame
import tech.pegasys.pantheon.ethereum.vm.OperationTracer
import tech.pegasys.pantheon.util.bytes.BytesValue
import java.util.ArrayDeque

class EVMTransactionProcessor(
    val gasCalculator: GasCalculator,
    val transactionValidator: TransactionValidator,
    val contractCreationProcessor: AbstractMessageProcessor,
    val messageCallProcessor: AbstractMessageProcessor,
    val clearEmptyAccounts: Boolean
) {

    private val logger = KotlinLogging.logger {}

    class Result(
        val status: Status,
        val logs: LogSeries,
        val gasRemaining: Long,
        val output: BytesValue,
        val validationResult: ValidationResult<TransactionInvalidReason>
    ) {

        companion object {
            fun invalid(
                validationResult: ValidationResult<TransactionInvalidReason>
            ): Result {
                return Result(
                    Status.INVALID,
                    LogSeries.empty(),
                    -1,
                    BytesValue.EMPTY,
                    validationResult
                )
            }

            fun failed(
                gasRemaining: Long,
                validationResult: ValidationResult<TransactionInvalidReason>
            ): Result {
                return Result(
                    Status.FAILED,
                    LogSeries.empty(),
                    gasRemaining,
                    BytesValue.EMPTY,
                    validationResult
                )
            }

            fun successful(
                logs: LogSeries,
                gasRemaining: Long,
                output: BytesValue,
                validationResult: ValidationResult<TransactionInvalidReason>
            ): Result {
                return Result(Status.SUCCESSFUL, logs, gasRemaining, output, validationResult)
            }
        }
    }

    fun processTransaction(
        blockchain: Blockchain,
        worldState: WorldUpdater,
        blockHeader: ProcessableBlockHeader,
        transaction: Transaction,
        miningBeneficiary: Address,
        operationTracer: OperationTracer,
        blockHashLookup: BlockHashLookup,
        isPersistingState: Boolean
    ): Result {

        logger.trace("Starting execution of {}", transaction)

        val senderAddress = transaction.sender
        val sender = worldState.getOrCreate(senderAddress)
        val validationResult = let {
            ValidationResult.valid<TransactionInvalidReason>()
        }

//        var validationResult = transactionValidator.validate(transaction)
//        // Make sure the transaction is intrinsically valid before trying to
//        // compare against a sender account (because the transaction may not
//        // be signed correctly to extract the sender).
//        if (!validationResult.isValid()) {
//            logger.warn("Invalid transaction: {}", validationResult.getErrorMessage())
//            return Result.invalid(validationResult)
//        }

//        validationResult = transactionValidator.validateForSender(transaction, sender, false)
//        if (!validationResult.isValid()) {
//            logger.warn("Invalid transaction: {}", validationResult.getErrorMessage())
//            return Result.invalid(validationResult)
//        }

        val previousNonce = sender.incrementNonce()
        logger.trace(
            "Incremented sender {} nonce ({} -> {})", senderAddress, previousNonce, sender.nonce
        )

        val upfrontGasCost = transaction.upfrontGasCost
        logger.trace(
            "Upfront gas cost {}",
            upfrontGasCost)

        val intrinsicGas = gasCalculator.transactionIntrinsicGasCost(transaction)
        val gasAvailable = Gas.of(transaction.gasLimit).minus(intrinsicGas)
        logger.trace(
            "Gas available for execution {} = {} - {} (limit - intrinsic)",
            gasAvailable,
            transaction.gasLimit,
            intrinsicGas
        )

        val worldUpdater = worldState.updater()
        val initialFrame: MessageFrame
        val messageFrameStack = ArrayDeque<MessageFrame>()
        if (transaction.isContractCreation) {
            val contractAddress = Address.contractAddress(senderAddress, sender.nonce - 1L)

            initialFrame = MessageFrame.builder()
                .type(MessageFrame.Type.CONTRACT_CREATION)
                .messageFrameStack(messageFrameStack)
                .blockchain(blockchain)
                .worldState(worldUpdater.updater())
                .initialGas(gasAvailable)
                .address(contractAddress)
                .originator(senderAddress)
                .contract(contractAddress)
                .gasPrice(transaction.gasPrice)
                .inputData(BytesValue.EMPTY)
                .sender(senderAddress)
                .value(transaction.value)
                .apparentValue(transaction.value)
                .code(Code(transaction.payload))
                .blockHeader(blockHeader)
                .depth(0)
                .completer { }
                .miningBeneficiary(miningBeneficiary)
                .blockHashLookup(blockHashLookup)
                .isPersistingState(isPersistingState)
                .maxStackSize(MessageFrame.DEFAULT_MAX_STACK_SIZE)
                .build()
        } else {
            val to = transaction.to.get()
            val contract = worldState.get(to)

            initialFrame = MessageFrame.builder()
                .type(MessageFrame.Type.MESSAGE_CALL)
                .messageFrameStack(messageFrameStack)
                .blockchain(blockchain)
                .worldState(worldUpdater.updater())
                .initialGas(gasAvailable)
                .address(to)
                .originator(senderAddress)
                .contract(to)
                .gasPrice(transaction.gasPrice)
                .inputData(transaction.payload)
                .sender(senderAddress)
                .value(transaction.value)
                .apparentValue(transaction.value)
                .code(Code(if (contract != null) contract.code else BytesValue.EMPTY))
                .blockHeader(blockHeader)
                .depth(0)
                .completer { }
                .miningBeneficiary(miningBeneficiary)
                .blockHashLookup(blockHashLookup)
                .maxStackSize(MessageFrame.DEFAULT_MAX_STACK_SIZE)
                .isPersistingState(isPersistingState)
                .build()
        }

        messageFrameStack.addFirst(initialFrame)

        while (!messageFrameStack.isEmpty()) {
            process(messageFrameStack.peekFirst(), operationTracer)
        }

        if (initialFrame.state == MessageFrame.State.COMPLETED_SUCCESS) {
            worldUpdater.commit()
        }

        logger.trace(
            "Gas used by transaction: {}, by message call/contract creation: {}",
            { Gas.of(transaction.gasLimit).minus(initialFrame.remainingGas) },
            { gasAvailable.minus(initialFrame.remainingGas) })

        // Refund the sender by what we should and pay the miner fee (note that we're doing them one
        // after the other so that if it is the same account somehow, we end up with the right result)
        val selfDestructRefund =
            gasCalculator.selfDestructRefundAmount.times(initialFrame.selfDestructs.size.toLong())
        val refundGas = initialFrame.gasRefund.plus(selfDestructRefund)
        val refunded = refunded(transaction, initialFrame.remainingGas, refundGas)
        val refundedWei = refunded.priceFor(transaction.gasPrice)
        sender.incrementBalance(refundedWei)

        val coinbase = worldState.getOrCreate(miningBeneficiary)
        val coinbaseFee = Gas.of(transaction.gasLimit).minus(refunded)
        val coinbaseWei = coinbaseFee.priceFor(transaction.gasPrice)
        coinbase.incrementBalance(coinbaseWei)

        initialFrame.selfDestructs.forEach { worldState.deleteAccount(it) }

        if (clearEmptyAccounts) {
            clearEmptyAccounts(worldState)
        }

        return if (initialFrame.state == MessageFrame.State.COMPLETED_SUCCESS) {
            Result.successful(
                initialFrame.logs,
                refunded.toLong(),
                initialFrame.outputData,
                validationResult
            )
        } else {
            Result.failed(refunded.toLong(), validationResult)
        }
    }

    private fun process(frame: MessageFrame, operationTracer: OperationTracer) {
        val executor = getMessageProcessor(frame.type)

        executor.process(frame, operationTracer)
    }

    private fun getMessageProcessor(type: MessageFrame.Type): AbstractMessageProcessor {
        return when (type) {
            MessageFrame.Type.MESSAGE_CALL -> messageCallProcessor
            MessageFrame.Type.CONTRACT_CREATION -> contractCreationProcessor
            else ->
                throw IllegalStateException("Request for unsupported message processor type $type")
        }
    }

    private fun refunded(
        transaction: Transaction,
        gasRemaining: Gas,
        gasRefund: Gas
    ): Gas {
        // Integer truncation takes care of the the floor calculation needed after the divide.
        val maxRefundAllowance = Gas.of(transaction.gasLimit).minus(gasRemaining).dividedBy(2)
        val refundAllowance = maxRefundAllowance.min(gasRefund)
        return gasRemaining.plus(refundAllowance)
    }

    private fun clearEmptyAccounts(worldState: WorldUpdater) {
        worldState.touchedAccounts.stream()
            .filter { it.isEmpty }
            .forEach { a -> worldState.deleteAccount(a.address) }
    }
}
