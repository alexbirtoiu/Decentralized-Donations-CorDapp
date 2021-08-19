package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.IOUMoneyContract
import com.template.contracts.IOUTokenContract
import com.template.states.IOUMoney
import com.template.states.IOUToken
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration

@InitiatingFlow
@StartableByRPC
class IOUMoneyIssueFlow(
    val iouTokenId : UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(iouTokenId))
        val iouTokenAndRef = serviceHub.vaultService.queryBy(IOUToken::class.java, criteria).states.single()
        val iouToken = iouTokenAndRef.state.data

        if(iouToken.lender != ourIdentity) {
            throw FlowException("Only the lender can issue IOUMoney")
        }

        val iouMoney = IOUMoney(iouToken.borrower, iouToken.lender, iouToken.donated)

        val timeWindow = TimeWindow.fromOnly(iouToken.expirationDate.plus(Duration.ofSeconds(1)))

        val txBuilder = TransactionBuilder(notary)
            .addInputState(iouTokenAndRef)
            .addOutputState(iouMoney)
            .addCommand(IOUTokenContract.Commands.Obligate(), listOf(iouToken.lender.owningKey, iouToken.borrower.owningKey))
            .addCommand(IOUMoneyContract.Commands.Obligate(), listOf(iouToken.lender.owningKey, iouToken.borrower.owningKey))
            .setTimeWindow(timeWindow)

        txBuilder.verify(serviceHub)

        val borrowerSession = initiateFlow(iouToken.borrower)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(borrowerSession)))

        return subFlow(FinalityFlow(stx, listOf(borrowerSession)))
    }
}