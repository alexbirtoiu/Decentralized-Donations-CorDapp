package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.template.states.Cause
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap


@InitiatedBy(DonateFlow::class)
class DonateFlowResponder(
    val counterpartySession : FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val amount = counterpartySession.receive<Amount<IssuedTokenType>>().unwrap{it}
        val causeId = counterpartySession.receive<UniqueIdentifier>().unwrap{it}
        val criteria = LinearStateQueryCriteria(linearId = listOf(causeId))
        val causeStateIn = serviceHub.vaultService.queryBy(Cause::class.java, criteria).states.single()

        subFlow(SendStateAndRefFlow(counterpartySession, listOf(causeStateIn)))

        val causeStateOut = (causeStateIn.state.data).receive(amount)

        counterpartySession.send(causeStateOut)

        counterpartySession.send(causeStateOut.getIOUToken(counterpartySession.counterparty, amount))

        subFlow(object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
               // Additional sanity checks for donate flows
            }

        })

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}