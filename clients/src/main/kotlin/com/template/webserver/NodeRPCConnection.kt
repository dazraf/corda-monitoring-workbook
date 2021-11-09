package com.template.webserver

import net.corda.client.jackson.JacksonSupport
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.GracefulReconnect
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.contextLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"

/**
 * Wraps an RPC connection to a Corda node.
 *
 * The RPC connection is configured using command line arguments.
 *
 * @param host The host of the node we are connecting to.
 * @param rpcPort The RPC port of the node we are connecting to.
 * @param username The username for logging into the RPC client.
 * @param password The password for logging into the RPC client.
 * @property proxy The RPC proxy.
 */
@Component
open class NodeRPCConnection(
    @Value("\${$CORDA_NODE_HOST}") private val host: String,
    @Value("\${$CORDA_USER_NAME}") private val username: String,
    @Value("\${$CORDA_USER_PASSWORD}") private val password: String,
    @Value("\${$CORDA_RPC_PORT}") private val rpcPort: Int
) : AutoCloseable {
    companion object {
        private val logger = contextLogger()
    }

    lateinit var rpcConnection: CordaRPCConnection
        private set
    val proxy: CordaRPCOps get() = rpcConnection.proxy
    val objectMapper get() = JacksonSupport.createDefaultMapper(proxy)

    @PostConstruct
    fun initialiseNodeRPCConnection() {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val configuration = CordaRPCClientConfiguration(
            connectionMaxRetryInterval = ofMinutes(3),
            minimumServerProtocolVersion = 4,
            trackRpcCallSites = true,
            reapInterval = ofSeconds(1),
            observationExecutorPoolSize = 4,
            cacheConcurrencyLevel = 1,
            connectionRetryInterval = ofSeconds(1),
            connectionRetryIntervalMultiplier = 1.0,
            maxReconnectAttempts = -1, // forever
            maxFileSize = 10485760,
            deduplicationCacheExpiry = ofDays(1)
        )
        val rpcClient = CordaRPCClient(rpcAddress, configuration)
        val gracefulReconnect = GracefulReconnect({ logger.info("on disconnect") }, { logger.info("on reconnect") })
        rpcConnection = rpcClient.start(username, password, gracefulReconnect)
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}