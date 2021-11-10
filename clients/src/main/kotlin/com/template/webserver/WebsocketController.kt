package com.template.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import com.template.states.TemplateState
import net.corda.core.node.services.Vault
import net.corda.core.utilities.contextLogger
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import rx.Subscriber
import rx.Subscription

import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

import org.springframework.web.socket.config.annotation.WebSocketConfigurer

import org.springframework.web.socket.config.annotation.EnableWebSocket

@Component
class WebsocketController(private val rpcConnection: NodeRPCConnection) :
    TextWebSocketHandler() {
    companion object {
        private val logger = contextLogger()
    }
    private val objectMapper get() = rpcConnection.objectMapper

    private val sessionSubscriptions = mutableMapOf<String, Subscription>()
    private val sessions = mutableMapOf<String, WebSocketSession>()
    private val proxy get() = rpcConnection.proxy

    @Throws(Exception::class)
    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        subscribeForVaultQuery(session)
    }

    fun sendProgressUpdate(message: String) {
        sessions.values.forEach {
            it.sendMessage(TextMessage(message))
        }
    }

    private fun subscribeForVaultQuery(session: WebSocketSession) {
        val dataFeed = proxy.vaultTrack(TemplateState::class.java)
        sessionSubscriptions[session.id] =
            dataFeed.updates.subscribe(object : Subscriber<Vault.Update<TemplateState>>() {
                override fun onCompleted() {
                    try {
                        logger.info("query completed - closing output stream")
                        session.close(CloseStatus.NORMAL)
                    } catch (err: Throwable) { // never allow exceptions to bubble up to corda rpc lib
                        logger.error("failed to close stream", err)
                        // the subscription will close because this is a complete message
                        // no tidy up required
                    }
                }

                override fun onError(e: Throwable) {
                    try {
                        logger.error("received error from vault query", e)
                        session.sendMessage(TextMessage("Error: ${e.message}")) // in prod do not expose internal messages
                        session.close(CloseStatus.SERVER_ERROR)
                    } catch (err: Throwable) {
                        logger.error("failed to write error to output stream", err)
                    }
                }

                override fun onNext(t: Vault.Update<TemplateState>) {
                    try {
                        logger.info("received vault notification $t")
                        val msg = objectMapper.writeValueAsString(t.produced.map { it.state.data })
                        session.sendMessage(TextMessage("⭕ $msg"))
                    } catch (err: Throwable) {
                        // we failed to write to the http stream
                        logger.error("failed to write to websocket", err)
                        if (!isUnsubscribed) {
                            logger.info("unsubscribing ${this.hashCode()}")
                            try {
                                this.unsubscribe()
                            } catch (err: Throwable) {
                                // belt and braces to avoid any exception flowing up into corda rpc lib
                            }
                            try {
                                sessionSubscriptions.remove(session.id)
                                session.close(CloseStatus.SERVER_ERROR)
                            } catch (err: Throwable) {
                                //
                            }
                        }
                    }
                }
            })
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session.id)
        sessionSubscriptions.computeIfPresent(session.id) { _, subscription ->
            try {
                if (!subscription.isUnsubscribed) {
                    subscription.unsubscribe()
                }
            } catch(err: Throwable) {
                logger.error("failed to unsubscribe", err)
            }

            null
        }
    }
}

@Configuration
@EnableWebSocket
open class WebSocketConfig(private val controller: WebsocketController) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(controller, "/api/query")
    }
}