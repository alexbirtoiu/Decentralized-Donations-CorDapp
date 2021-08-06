package com.template

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokensHandler
import com.template.flows.*
import com.template.states.Cause
import com.template.states.IOUToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.LinkedHashMap


class FlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var donor: StartedMockNode
    lateinit var organization: StartedMockNode
    lateinit var bank: StartedMockNode

    lateinit var donorParty : Party
    lateinit var organizationParty : Party
    lateinit var bankParty : Party

    val tokenType = TokenType("Org", 0)
    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.template.contracts", "com.template.flows", "com.r3.corda.lib.tokens"),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        donor = mockNetwork.createNode(MockNodeParameters())
        organization = mockNetwork.createNode(MockNodeParameters())
        bank = mockNetwork.createNode(MockNodeParameters())

        donorParty = donor.info.chooseIdentityAndCert().party
        organizationParty = organization.info.chooseIdentityAndCert().party
        bankParty = bank.info.chooseIdentityAndCert().party

        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    @Test
    fun issueMoneyTest() {
        val flow = IssueFungibleTokenFlow(10.GBP, donorParty)
        val future = bank.startFlow(flow)

        mockNetwork.runNetwork()

        val ptx = future.getOrThrow()

        assert (ptx.tx.inputs.isEmpty())
        assert (ptx.tx.outputs.size == 1)

        val money = ptx.tx.outputs.single().data as FungibleToken

        assert (money.tokenType.tokenIdentifier == "GBP")
        assert (money.amount.quantity == 1000L)
        assert (money.holder == donorParty)
        assert (money.issuer == bankParty)
    }

    @Test
    fun redeemMoneyTest() {
        val issueFlow = IssueFungibleTokenFlow(10.GBP, donorParty)
        val future1 = bank.startFlow(issueFlow)
        mockNetwork.runNetwork()

        val stx = future1.getOrThrow()
        val redeemFlow = RedeemMoneyFlow(5.GBP issuedBy bankParty)
        val future2 = donor.startFlow(redeemFlow)
        mockNetwork.runNetwork()

        val ptx = future2.getOrThrow()

        assert (ptx.tx.inputs.size == 1)
        assert (ptx.tx.outputs.size == 1)

        val moneyOut = ptx.tx.outputs.single().data as FungibleToken

        assert (ptx.tx.inputs.single() == StateRef(stx.id, 0) )

        assert (moneyOut.tokenType.tokenIdentifier == "GBP")
        assert (moneyOut.amount.quantity == 500L)
        assert (moneyOut.holder == donorParty)
        assert (moneyOut.issuer == bankParty)
    }

    @Test
    fun requestDonationFlow() {
        val issueFlow = IssueFungibleTokenFlow(10.GBP, donorParty)
        val future1 = bank.startFlow(issueFlow)
        mockNetwork.runNetwork()
        future1.getOrThrow()

        val iouToken = IOUToken(donorParty, null, 5 of tokenType issuedBy organizationParty, 10.GBP issuedBy bankParty, UniqueIdentifier())
        val flow = RequestDonationFlow(10.GBP issuedBy bankParty, donorParty, iouToken)
        val future = organization.startFlow(flow)
        mockNetwork.runNetwork()
        val ptx = future.getOrThrow()

    }

    @Test
    fun issueCauseFlow() {
        val cause = Cause("Project", "description", 200.GBP issuedBy bankParty, 40 of tokenType issuedBy organizationParty, 0.GBP issuedBy bankParty)
        val issueCauseFlow = IssueCauseFlow(cause)
        val future1 = organization.startFlow(issueCauseFlow)
        mockNetwork.runNetwork()
        future1.getOrThrow()
    }

    @Test
    fun donateFlow() {
        val cause = Cause("Project", "description", 200.GBP issuedBy bankParty, 40 of tokenType issuedBy organizationParty, 0.GBP issuedBy bankParty)
        val issueCauseFlow = IssueCauseFlow(cause)
        val future1 = organization.startFlow(issueCauseFlow)
        mockNetwork.runNetwork()
        future1.getOrThrow()

        val issueMoneyFlow = IssueFungibleTokenFlow(40.GBP, donorParty)
        val future2 = bank.startFlow(issueMoneyFlow)
        mockNetwork.runNetwork()
        future2.getOrThrow()

        val donateFlow = DonateFlow(organizationParty, 40.GBP issuedBy bankParty, cause.linearId)
        val future3 = donor.startFlow(donateFlow)
        mockNetwork.runNetwork()
        future3.getOrThrow()
    }
}