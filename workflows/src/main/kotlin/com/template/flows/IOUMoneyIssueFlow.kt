package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class IOUMoneyIssueFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        TODO("Not yet implemented")
    }
}