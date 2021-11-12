package com.template.webserver

import com.template.flows.SimpleTemplateFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.FlowProgressHandle
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
import org.springframework.web.server.ResponseStatusException

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

  // do not cache because the proxy may become replaced due to reconnection event
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

  data class SimpleNodeInfo(
    val addresses: List<NetworkHostAndPort>,
    val names: List<String>
  )

  private fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo =
    SimpleNodeInfo(addresses, legalIdentitiesAndCerts.map {
      it.name.toString()
    })

  @PostMapping(value = ["/flow"], produces = [TEXT_PLAIN_VALUE])
  private fun startFlow(
    @RequestHeader("clientRequestId") clientRequestId: String,
    @RequestHeader("recipient") recipient: String,
    @RequestHeader("message") message: String
  ): String {
    try {
      val recipientParty = parsePartyFromName(recipient)
      val flowHandle = invokeFlow(recipientParty, message)
      flowHandle.registerProgressUpdates(clientRequestId)
      return flowHandle.returnValue.getOrThrow().tx.id.toString()
    } catch (err: Throwable) {
      logger.error("failed to execute flow", err)
      when (err) {
        is ResponseStatusException -> throw err
        else -> throw ResponseStatusException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
          err.message
        )
      }
    }
  }

  private fun FlowProgressHandle<*>.registerProgressUpdates(
    clientRequestId: String
  ) {
    progress.subscribe {
      try {
        logger.info("flow handle $id progress: $it`")
        websocketController.sendProgressUpdate("Flow $clientRequestId: $it")
      } catch (err: Throwable) {
        logger.error("failed to send progress notification: $it", err)
      }
    }
  }

  private fun invokeFlow(recipientParty: Party, message: String) =
    proxy.startTrackedFlowDynamic(
      SimpleTemplateFlow::class.java,
      recipientParty,
      message
    )

  private fun parsePartyFromName(recipient: String) =
    proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(recipient))
      ?: error("unknown party $recipient")

  @EventListener(ApplicationReadyEvent::class)
  fun postStartupPrint() {
    logger.info("Server started on http://localhost:${environment["local.server.port"]}")
  }
}
