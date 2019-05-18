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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import java.math.BigInteger

class EvmTest {

    val PRIVATE_KEY = "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"

    @Test
    fun transactionPassesTroughEvm() {

        val keypair = ECKeyPair.create(BigInteger(PRIVATE_KEY, 16))

        val evm = EVM(Credentials.create(keypair))

        val result = evm.run("0x627306090abaB3A6e1400e9345bC60c78a8BEf57", "", BigInteger.ZERO)

        val result1 = evm.run("0x627306090abaB3A6e1400e9345bC60c78a8BEf57", "", BigInteger.ZERO)

        assertTrue(result.isStatusOK)
        assertTrue(result1.isStatusOK)
    }
}
