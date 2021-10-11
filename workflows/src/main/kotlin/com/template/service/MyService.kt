package com.template.service

import net.corda.core.crypto.toStringShort
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class MyService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

  init {
    println("App has started")
    println("Expect to see the following Artemis queues:")
    serviceHub.myInfo.legalIdentitiesAndCerts.map { partyAndCert ->
      println("${partyAndCert.party.name}: p2p.inbound.${partyAndCert.owningKey.toStringShort()}")
    }
  }
}
