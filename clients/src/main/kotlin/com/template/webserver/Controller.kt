package com.template.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.util.MimeTypeUtils.*
import com.template.flows.SimpleTemplateFlow
import com.template.states.TemplateState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.Update
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import rx.Subscriber
import java.io.IOException
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api") // The paths for HTTP requests are relative to this base path.
class Controller(
    private val rpc: NodeRPCConnection,
    private val environment: Environment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy get() = rpc.proxy

    @GetMapping(value = ["/network"], produces = [APPLICATION_JSON_VALUE])
    private fun network(): List<SimpleNodeInfo> {
        return rpc.proxy.networkMapSnapshot().map {
            it.toSimpleNodeInfo()
        }
    }

    @GetMapping(value = ["/my-info"], produces = [APPLICATION_JSON_VALUE])
    private fun myInfo(): SimpleNodeInfo {
        return this.rpc.proxy.nodeInfo().toSimpleNodeInfo()
    }

    data class SimpleNodeInfo(val addresses: List<NetworkHostAndPort>, val names: List<String>)

    private fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo = SimpleNodeInfo(addresses, legalIdentitiesAndCerts.map {
        it.name.toString()
    })

    @PostMapping(value = ["/flow"], produces = [TEXT_PLAIN_VALUE])
    private fun startFlow(@RequestHeader("recipient") recipient: String, @RequestHeader("message") message: String): String {
        val recipientParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(recipient))
        val secureHash =
            proxy.startFlowDynamic(SimpleTemplateFlow::class.java, recipientParty, message).returnValue.getOrThrow().id
        return secureHash.toString()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun postStartupPrint() {
        logger.info("Server started on http://localhost:${environment["local.server.port"]}")
    }
}
