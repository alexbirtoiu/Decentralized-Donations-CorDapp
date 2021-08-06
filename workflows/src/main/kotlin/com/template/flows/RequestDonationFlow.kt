package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.template.contracts.IOUTokenContract
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@Deprecated("This flow is not currently used")
@InitiatingFlow
@StartableByRPC
class RequestDonationFlow(
    val totalAmount : Amount<IssuedTokenType>,
    val donor : Party,
    val iouToken : IOUToken
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val donorSession = initiateFlow(donor)

        donorSession.send(totalAmount.withoutIssuer())

        val cashIn = subFlow(ReceiveStateAndRefFlow<FungibleToken>(donorSession))

        val cashOut = donorSession.receive(List::class.java).unwrap { it as List<FungibleToken>}

        val txBuilder = TransactionBuilder(notary)
            .addOutputState(iouToken, IOUTokenContract.ID)
            .addCommand(IOUTokenContract.Commands.Donate(), iouToken.participants.map{it.owningKey})

        addMoveTokens(txBuilder, cashIn, cashOut)

        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(donorSession)))

        return subFlow(FinalityFlow(stx, donorSession))
    }
}