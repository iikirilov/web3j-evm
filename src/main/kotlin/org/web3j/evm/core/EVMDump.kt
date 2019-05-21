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

import org.web3j.protocol.core.methods.response.TransactionReceipt
import tech.pegasys.pantheon.ethereum.core.Gas
import tech.pegasys.pantheon.ethereum.debug.TraceFrame
import tech.pegasys.pantheon.ethereum.vm.ExceptionalHaltReason
import java.util.EnumSet

class EVMDump(
    val transacitonReceipt: TransactionReceipt,
    val output: String,
    val traceFrames: List<EVMTraceFrame>
)

class EVMTraceFrame(
    val pc: Int,
    val opcode: String,
    val gasRemaining: Long,
    val cost: Long,
    val depth: Int,
    val exceptionalHaltReasons: EnumSet<ExceptionalHaltReason>,
    val stack: List<String>,
    val memory: List<String>
//    val storage: Map<UInt256, UInt256>
) {
    constructor(traceFrame: TraceFrame):
            this(
                traceFrame.pc,
                traceFrame.opcode,
                traceFrame.gasRemaining.toLong(),
                traceFrame.gasCost.orElse(Gas.ZERO).toLong(),
                traceFrame.depth,
                traceFrame.exceptionalHaltReasons,
                traceFrame.stack.orElse(arrayOf()).asList().map { it.toString() },
                traceFrame.memory.orElse(arrayOf()).asList().map { it.toString() })
//                traceFrame.storage.orElse(mapOf()))
//    constructor(traceFrame: TraceFrame): this(traceFrame.pc, traceFrame.opcode, traceFrame.gasRemaining, traceFrame.depth, traceFrame.exceptionalHaltReasons)
}
