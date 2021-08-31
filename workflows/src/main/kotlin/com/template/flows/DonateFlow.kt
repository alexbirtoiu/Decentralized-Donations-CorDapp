package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.template.contracts.CauseContract
import com.template.contracts.IOUTokenContract
import com.template.states.Cause
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.time.Duration
import sun.awt.CausedFocusEvent

@InitiatingFlow
@StartableByRPC
class DonateFlow(
    val organization : Party,
    val amount : Amount<IssuedTokenType>,
    val causeId : UniqueIdentifier
    ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val organizationSession = initiateFlow(organization)

        organizationSession.send(amount)

        organizationSession.send(causeId)

        val causeStateIn = subFlow(ReceiveStateAndRefFlow<Cause>(organizationSession)).single()

        val causeStateOut = organizationSession.receive(Cause::class.java).unwrap{it}

        val iouToken = organizationSession.receive(IOUToken::class.java).unwrap{it}

        val timeWindow = TimeWindow.untilOnly(causeStateIn.state.data.timeLimit.minus(Duration.ofSeconds(1)))

        val txBuilder = TransactionBuilder(notary)
            .addInputState(causeStateIn)
            .addOutputState(causeStateOut, CauseContract.ID)
            .addOutputState(iouToken, IOUTokenContract.ID)
            .addCommand(CauseContract.Commands.Donate(), causeStateOut.participants.map{it.owningKey})
            .addCommand(IOUTokenContract.Commands.Donate(), iouToken.participants.map{it.owningKey})
            .setTimeWindow(timeWindow)

        val cashInputsOutputs = DatabaseTokenSelection(serviceHub)
            .generateMove(listOf(Pair(organization, amount.withoutIssuer())), ourIdentity, TokenQueryBy(amount.token.issuer))

        addMoveTokens(txBuilder, cashInputsOutputs.first, cashInputsOutputs.second)

        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(organizationSession)))

        return subFlow(FinalityFlow(stx, organizationSession))
    }
}