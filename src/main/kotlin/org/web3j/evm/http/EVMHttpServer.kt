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

import de.nielsfalk.ktor.swagger.DefaultValue
import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Contact
import de.nielsfalk.ktor.swagger.version.shared.Group
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.web3j.evm.core.EVM
import org.web3j.evm.core.EVMDump
import java.math.BigInteger

class EVMHttpServer(val evm: EVM) {

    @Group("evm operations")
    @Location("/run")
    class Run(
        @DefaultValue("0x")
        val data: String,
        @DefaultValue("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73")
        val to: String,
        @DefaultValue("0")
        val value: Int
    )

    init {
        val server = embeddedServer(Netty, 8080) {
            install(DefaultHeaders)
            install(Compression)
            install(CallLogging)
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            install(Locations)
            install(SwaggerSupport) {
                forwardRoot = true
                val information = Information(
                    version = "0.1",
                    title = "web3j-evm",
                    contact = Contact(
                        name = "Web3 Labs",
                        email = "hi@web3labs.com"
                    )
                )
                swagger = Swagger().apply {
                    info = information
                }
            }
            routing {
                post<Run, Run>("run".responds(ok<EVMDump>())) { _, run ->
                    call.respond(evm.run(
                        run.to,
                        run.data,
                        BigInteger.valueOf(run.value.toLong())))
                }
            }
        }
        server.start(wait = true)
    }
}
