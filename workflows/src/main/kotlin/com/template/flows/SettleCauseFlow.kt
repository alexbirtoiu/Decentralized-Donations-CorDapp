package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.redeem.addTokensToRedeem
import com.template.contracts.CauseContract
import com.template.states.Cause
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class SettleCauseFlow(
    val causeId : UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(causeId))
        val causeStateIn = serviceHub.vaultService.queryBy(Cause::class.java, criteria).states.single()

        val txBuilder = TransactionBuilder(notary)
            .addInputState(causeStateIn)
            .addCommand(CauseContract.Commands.Settle(), listOf(ourIdentity.owningKey))


        val amount = causeStateIn.state.data.neededAmount
        val cashInputs = DatabaseTokenSelection(serviceHub)
            .selectTokens(ourIdentity.owningKey, amount.withoutIssuer())

        addTokensToRedeem(txBuilder, cashInputs)

        txBuilder.verify(serviceHub)

        val issuerSession = initiateFlow(amount.token.issuer)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(issuerSession)))

        return subFlow(FinalityFlow(stx, listOf(issuerSession)))
    }
}