package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.template.contracts.IOUMoneyContract
import com.template.states.IOUMoney
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IOUMoneySettleFlow(
    val iouId :UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(iouId))
        val iouMoneyAndRef = serviceHub.vaultService.queryBy(IOUMoney::class.java, criteria).states.single()
        val iouMoney = iouMoneyAndRef.state.data

        if(iouMoney.borrower != ourIdentity) {
            throw FlowException("Only the borrower can settle an IOUMoney")
        }

        val txBuilder = TransactionBuilder(notary)
            .addInputState(iouMoneyAndRef)
            .addCommand(IOUMoneyContract.Commands.Settle(), iouMoney.participants.map{it.owningKey})

        val cashInputsOutputs = DatabaseTokenSelection(serviceHub)
            .generateMove(listOf(Pair(iouMoney.lender, iouMoney.amount.withoutIssuer())), ourIdentity, TokenQueryBy(iouMoney.amount.token.issuer))

        addMoveTokens(txBuilder, cashInputsOutputs.first, cashInputsOutputs.second)

        txBuilder.verify(serviceHub)

        val donorSession = initiateFlow(iouMoney.lender)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(donorSession)))

        return subFlow(FinalityFlow(stx, listOf(donorSession)))
    }
}