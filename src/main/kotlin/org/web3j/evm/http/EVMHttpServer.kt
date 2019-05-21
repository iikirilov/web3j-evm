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

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.badRequest
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
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.web3j.evm.core.EVM
import org.web3j.evm.core.EVMDump
import org.web3j.evm.core.EVMException
import java.math.BigInteger

class EVMHttpServer(val evm: EVM) {
    private val logger = KotlinLogging.logger {}

    @Group("evm operations")
    @Location("/run")
    class RunParam

    init {
        val server = embeddedServer(Netty, 8080) {
            install(DefaultHeaders)
            install(Compression)
            install(CallLogging)
            install(ContentNegotiation) {
                gson { setPrettyPrinting() }
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
                post<RunParam, RunBody>(
                    "run".responds(
                        ok<EVMDump>(),
                        badRequest<ParamInvalidReason>())) { _, run ->
                    logger.info { "validating request $run" }
                    validateRunBody(run).either({ runBlocking {
                        try {
                            call.respond(
                                evm.run(
                                    run.to,
                                    run.data,
                                    BigInteger.valueOf(run.value.toLong())))
                        } catch (e: EVMException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                e.result.validationResult)
                        }
                    } }, { errorReason -> runBlocking {
                        call.respond(HttpStatusCode.BadRequest, errorReason)
                    } })
                }
            }
        }
        server.start(wait = true)
    }
}
