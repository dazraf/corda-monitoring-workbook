package com.template.webserver

import com.template.flows.SimpleTemplateFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE
import org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE
import org.springframework.web.bind.annotation.*


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api") // The paths for HTTP requests are relative to this base path.
class Controller(
    private val rpc: NodeRPCConnection,
    private val websocketController: WebsocketController,
    private val environment: Environment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy get() = rpc.proxy

    @GetMapping(value = ["/name"], produces = [APPLICATION_JSON_VALUE])
    private fun name(): String {
        return rpc.proxy.nodeInfo().legalIdentities.first().name.organisation
    }

    @GetMapping(value = ["/network"], produces = [APPLICATION_JSON_VALUE])
    private fun network(): List<SimpleNodeInfo> {
        return rpc.proxy.networkMapSnapshot().map {
            it.toSimpleNodeInfo()
        }
    }

    @GetMapping(value = ["/my-info"], produces = [APPLICATION_JSON_VALUE])
    private fun myInfo(): SimpleNodeInfo {
        return rpc.proxy.nodeInfo().toSimpleNodeInfo()
    }

    @GetMapping(value = ["/throw-on-notification"], produces = [APPLICATION_JSON_VALUE])
    private fun throwOnNotification(): Boolean {
        return websocketController.throwOnNotification
    }

    @PutMapping(value = ["/throw-on-notification"])
    private fun throwOnNotification(@RequestHeader("value") value: Boolean) {
        websocketController.throwOnNotification = value
    }

    data class SimpleNodeInfo(val addresses: List<NetworkHostAndPort>, val names: List<String>)

    private fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo = SimpleNodeInfo(addresses, legalIdentitiesAndCerts.map {
        it.name.toString()
    })

    @PostMapping(value = ["/flow"], produces = [TEXT_PLAIN_VALUE])
    private fun startFlow(
        @RequestHeader("clientRequestId") clientRequestId: String,
        @RequestHeader("recipient") recipient: String,
        @RequestHeader("message") message: String
    ): String {
        val recipientParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(recipient))
        val flowHandle = proxy.startTrackedFlowDynamic(SimpleTemplateFlow::class.java, recipientParty, message)
        flowHandle.progress.subscribe {
            logger.info("flow handle ${flowHandle.id} progress: $it`")
            websocketController.sendProgressUpdate("Flow $clientRequestId: $it")
        }
        return flowHandle.returnValue.getOrThrow().tx.id.toString()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun postStartupPrint() {
        logger.info("Server started on http://localhost:${environment["local.server.port"]}")
    }
}
