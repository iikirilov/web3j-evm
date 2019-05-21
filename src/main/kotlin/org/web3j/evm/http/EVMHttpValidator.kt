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
package org.web3j.evm.http

import tech.pegasys.pantheon.ethereum.core.Address
import tech.pegasys.pantheon.ethereum.mainnet.ValidationResult
import tech.pegasys.pantheon.util.bytes.BytesValue
import java.lang.IllegalArgumentException

fun validateRunBody(run: RunBody): ValidationResult<ParamInvalidReason> {
    try {
        BytesValue.fromHexString(run.data)
    } catch (e: Exception) {
        return ValidationResult.invalid(ParamInvalidReason("data", e.localizedMessage))
    }
    try {
        Address.fromHexString(run.to)
        if (run.to!!.length != 42) throw IllegalArgumentException("Address is 20 bytes long")
    } catch (e: Exception) {
        return ValidationResult.invalid(ParamInvalidReason("to", e.localizedMessage))
    }
    return ValidationResult.valid()
}
