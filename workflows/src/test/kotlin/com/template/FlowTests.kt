package com.template

import com.google.common.collect.ImmutableList
import com.template.flows.IssueFungibleTokenFlow
import com.template.flows.RedeemMoneyFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NetworkParameters
import net.corda.core.node.services.AttachmentId
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import java.time.Instant
import java.util.LinkedHashMap


class FlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("net.corda.training"),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = listOf(a, b, c)

        startedNodes.forEach{ it.registerInitiatedFlow(IssueFungibleTokenFlow::class.java)}
        startedNodes.forEach{ it.registerInitiatedFlow(RedeemMoneyFlow::class.java)}

        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

}