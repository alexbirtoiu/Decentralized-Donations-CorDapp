package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(IOUTokenSettleFlow::class)
class IOUTokenSettleResponderFlow(
    val counterpartySession : FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Check the attachments added to the transaction correspond to the cause
                // Additional sanity checks for IOUTokenSettle flows
            }
        })

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}