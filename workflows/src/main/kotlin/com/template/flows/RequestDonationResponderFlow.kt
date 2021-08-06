package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

@Deprecated("This flow is not currently used")
@InitiatedBy(RequestDonationFlow::class)
class RequestDonationResponderFlow(
    val counterpartySession : FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val amount = counterpartySession.receive<Amount<TokenType>>().unwrap{ it }

        val cashInputsOutputs = DatabaseTokenSelection(serviceHub)
            .generateMove(listOf(Pair(counterpartySession.counterparty, amount)), ourIdentity)

        subFlow(SendStateAndRefFlow(counterpartySession, cashInputsOutputs.first))

        counterpartySession.send(cashInputsOutputs.second)

        subFlow(object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Custom Logic to validate transaction.
            }
        })

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }

}