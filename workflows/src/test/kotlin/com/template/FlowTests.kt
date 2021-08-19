package com.template

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokensHandler
import com.template.flows.*
import com.template.states.Cause
import com.template.states.IOUMoney
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
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.LinkedHashMap


class FlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var donor: StartedMockNode
    lateinit var donor2: StartedMockNode
    lateinit var organization: StartedMockNode
    lateinit var bank: StartedMockNode

    lateinit var donorParty : Party
    lateinit var donor2Party : Party
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
        donor2 = mockNetwork.createNode(MockNodeParameters())

        donorParty = donor.info.chooseIdentityAndCert().party
        donor2Party = donor2.info.chooseIdentityAndCert().party
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

//    @Test
//    fun redeemMoneyTest() {
//        val issueFlow = IssueFungibleTokenFlow(10.GBP, donorParty)
//        val future1 = bank.startFlow(issueFlow)
//        mockNetwork.runNetwork()
//
//        val stx = future1.getOrThrow()
//        val redeemFlow = RedeemMoneyFlow(5.GBP issuedBy bankParty)
//        val future2 = donor.startFlow(redeemFlow)
//        mockNetwork.runNetwork()
//
//        val ptx = future2.getOrThrow()
//
//        assert (ptx.tx.inputs.size == 1)
//        assert (ptx.tx.outputs.size == 1)
//
//        val moneyOut = ptx.tx.outputs.single().data as FungibleToken
//
//        assert (ptx.tx.inputs.single() == StateRef(stx.id, 0) )
//
//        assert (moneyOut.tokenType.tokenIdentifier == "GBP")
//        assert (moneyOut.amount.quantity == 500L)
//        assert (moneyOut.holder == donorParty)
//        assert (moneyOut.issuer == bankParty)
//    }

//    @Test
//    fun requestDonationFlow() {
//        val issueFlow = IssueFungibleTokenFlow(10.GBP, donorParty)
//        val future1 = bank.startFlow(issueFlow)
//        mockNetwork.runNetwork()
//        future1.getOrThrow()
//
//        val iouToken = IOUToken(donorParty, null, 5 of tokenType issuedBy organizationParty, 10.GBP issuedBy bankParty, UniqueIdentifier())
//        val flow = RequestDonationFlow(10.GBP issuedBy bankParty, donorParty, iouToken)
//        val future = organization.startFlow(flow)
//        mockNetwork.runNetwork()
//        future.getOrThrow()
//
//    }

    @Test
    fun issueCauseFlow() {
        val cause = Cause("Project", "description", 200.GBP issuedBy bankParty, 40 of tokenType issuedBy organizationParty,  Instant.now().plus(
            Duration.ofDays(7)), Duration.ofMinutes(5))
        val issueCauseFlow = IssueCauseFlow(cause)
        val future1 = organization.startFlow(issueCauseFlow)
        mockNetwork.runNetwork()
        future1.getOrThrow()
    }

    @Test
    fun useCaseTest() {
        // Creating the Cause
        val cause = Cause("Project", "description", 200.GBP issuedBy bankParty, 40 of tokenType issuedBy organizationParty, Instant.now().plus(Duration.ofSeconds(10)), Duration.ofSeconds(0))
        val issueCauseFlow = IssueCauseFlow(cause)
        val future1 = organization.startFlow(issueCauseFlow)
        mockNetwork.runNetwork()
        future1.getOrThrow()

        // Issuing Money for the 2 donors
        val issueMoneyFlow = IssueFungibleTokenFlow(500.GBP, donorParty)
        val future2 = bank.startFlow(issueMoneyFlow)
        mockNetwork.runNetwork()
        future2.getOrThrow()

        val issueMoneyFlow2 = IssueFungibleTokenFlow(500.GBP, donor2Party)
        val future3 = bank.startFlow(issueMoneyFlow2)
        mockNetwork.runNetwork()
        future3.getOrThrow()

        // Donating both amounts to the organization
        val donateFlow = DonateFlow(organizationParty, 40.GBP issuedBy bankParty, cause.linearId)
        val future4 = donor.startFlow(donateFlow)
        mockNetwork.runNetwork()
        val donate1 = future4.getOrThrow()
        val iou1 = donate1.tx.outputsOfType<IOUToken>().single()

        val donateFlow2 = DonateFlow(organizationParty, 160.GBP issuedBy bankParty, cause.linearId)
        val future5 = donor2.startFlow(donateFlow2)
        mockNetwork.runNetwork()
        val donate2 =future5.getOrThrow()
        val iou2 = donate2.tx.outputsOfType<IOUToken>().single()

        // The organization settles the cause and redeems the money from the ledger
        val settleCauseFlow = SettleCauseFlow(cause.linearId)
        val future6 = organization.startFlow(settleCauseFlow)
        mockNetwork.runNetwork()
        future6.getOrThrow()

        // The organization creates
        val tokens = 40 of tokenType issuedBy organizationParty
        val issueTokensForCause = IssueFungibleTokenFlow(tokens.withoutIssuer(), organizationParty)
        val future7 = organization.startFlow(issueTokensForCause)
        future7.getOrThrow()

        // Settle both of the IOUs created by the two donating flows
        val settleIouTokenDonor = IOUTokenSettleFlow(iou1.linearId)
        val future8 = organization.startFlow(settleIouTokenDonor)
        mockNetwork.runNetwork()
        future8.getOrThrow()

        sleep(10000)

//        val settleIouTokenDonor2 = IOUTokenSettleFlow(iou2.linearId)
//        val future9 = organization.startFlow(settleIouTokenDonor2)
//        mockNetwork.runNetwork()
//        future9.getOrThrow()

        val issueIOUMoney = IOUMoneyIssueFlow(iou2.linearId)
        val future10 = donor2.startFlow(issueIOUMoney)
        mockNetwork.runNetwork()
        val obligate = future10.getOrThrow()
        val iouMoney = obligate.tx.outputsOfType<IOUMoney>().single()

        val issueMoneyOrganization = IssueFungibleTokenFlow(500.GBP, organizationParty)
        val future11 = bank.startFlow(issueMoneyOrganization)
        mockNetwork.runNetwork()
        future11.getOrThrow()

        val returnMoney = IOUMoneySettleFlow(iouMoney.linearId)
        val future12 = organization.startFlow(returnMoney)
        mockNetwork.runNetwork()
        future12.getOrThrow()
    }
}