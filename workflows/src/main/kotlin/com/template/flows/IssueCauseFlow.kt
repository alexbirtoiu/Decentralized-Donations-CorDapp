package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CauseContract
import com.template.states.Cause
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IssueCauseFlow(
    val cause : Cause
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val txBuilder = TransactionBuilder(notary)
            .addOutputState(cause, CauseContract.ID)
            .addCommand(CauseContract.Commands.Issue(), listOf(ourIdentity.owningKey))

        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf()))

        return subFlow(FinalityFlow(stx,listOf()))
    }
}