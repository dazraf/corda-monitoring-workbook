package com.template.service

import net.corda.core.crypto.toStringShort
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@Suppress("unused")
@CordaService
class CustomService(serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

  init {
    println("App has started.")
    println("Artemis should have the following queues - one per legal entity of the node:")
    serviceHub.myInfo.legalIdentitiesAndCerts.map { partyAndCert ->
      println("  ${partyAndCert.party.name}: p2p.inbound.${partyAndCert.owningKey.toStringShort()}")
    }
  }
}
