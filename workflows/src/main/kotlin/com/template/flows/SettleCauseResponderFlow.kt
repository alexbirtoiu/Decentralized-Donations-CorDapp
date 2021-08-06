package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(SettleCauseFlow::class)
class SettleCauseResponderFlow(
    val counterpartySession : FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
               // Additional sanity checks for SettleCause flows
            }
        })

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}