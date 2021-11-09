package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.util.MimeTypeUtils.*
import com.template.flows.SimpleTemplateFlow
import net.corda.core.node.NodeInfo
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import org.springframework.web.bind.annotation.PostMapping

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api") // The paths for HTTP requests are relative to this base path.
class Controller(private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

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
    private fun startFlow(): String {
        val tx = this.rpc.proxy.startFlowDynamic(SimpleTemplateFlow::class.java, this.rpc.proxy.nodeInfo().legalIdentities.first()).returnValue.getOrThrow()
        return tx.toString()
    }
}
