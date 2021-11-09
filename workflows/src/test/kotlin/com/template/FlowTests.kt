package com.template

import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.template.states.TemplateState
import java.util.concurrent.Future;
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import com.template.flows.SimpleTemplateFlow
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.utilities.getOrThrow


class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }
    @Test
    fun `DummyTest`() {
        val flow = SimpleTemplateFlow(b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()

        val result = future.getOrThrow()
        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = b.services.vaultService.queryBy(TemplateState::class.java, inputCriteria).states[0].state.data
    }
}
