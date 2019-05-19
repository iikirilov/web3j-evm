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
import tech.pegasys.pantheon.ethereum.core.Transaction
import tech.pegasys.pantheon.ethereum.mainnet.TransactionProcessor

class EVMDump(
    val transacitonReceipt: TransactionReceipt,
    val transaction: Transaction,
    val result: TransactionProcessor.Result
)