package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.template.contracts.IOUTokenContract
import com.template.states.Cause
import com.template.states.IOUToken
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IOUTokenSettleFlow(
    val iouId : UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(iouId))
        val iouTokenAndRef = serviceHub.vaultService.queryBy(IOUToken::class.java, criteria).states.single()
        val iouToken = iouTokenAndRef.state.data

        val txBuilder = TransactionBuilder(notary)
            .addInputState(iouTokenAndRef)
            .addCommand(IOUTokenContract.Commands.Settle(), iouToken.participants.map{it.owningKey})

        val cashInputsOutputs = DatabaseTokenSelection(serviceHub)
            .generateMove(listOf(Pair(iouToken.lender, iouToken.amount.withoutIssuer())), ourIdentity)

        addMoveTokens(txBuilder, cashInputsOutputs.first, cashInputsOutputs.second)

        txBuilder.verify(serviceHub)

        val donorFlow = initiateFlow(iouToken.lender)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(donorFlow)))

        return subFlow(FinalityFlow(stx, listOf(donorFlow)))


    }
}