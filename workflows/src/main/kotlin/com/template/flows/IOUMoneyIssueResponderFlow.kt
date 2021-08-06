package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(IOUMoneyIssueFlow::class)
class IOUMoneyIssueResponderFlow(
    val counterpartySession : FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        TODO("Not yet implemented")
    }
}