package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.IOUTokenContract
import com.template.states.Cause
import com.template.states.IOUToken
import com.template.states.RewardToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IOUTokenSettleFlow(
    val proof : List<AttachmentId>,
    val iouId : UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(iouId))
        val iouTokenAndRef = serviceHub.vaultService.queryBy(IOUToken::class.java, criteria).states.single()
        val iouToken = iouTokenAndRef.state.data

        if(iouToken.borrower != ourIdentity) {
            throw FlowException("Only the borrower can settle an IOUToken")
        }

        val rewardToken = RewardToken(proof, iouToken.amount heldBy ourIdentity)
        val tokenTx = subFlow(IssueTokens(listOf(rewardToken)))

        val issuedToken = tokenTx.tx.outputsOfType<RewardToken>().single()

        val movedToken = issuedToken.withNewHolder(iouToken.lender)

        val issuedTokenStateAndRef = StateAndRef(tokenTx.tx.outputs.single(), StateRef(tokenTx.id, 0))

        val txBuilder = TransactionBuilder(notary)
            .addInputState(issuedTokenStateAndRef)
            .addOutputState(movedToken, FungibleTokenContract.contractId)
            .addInputState(iouTokenAndRef)
            .addCommand(MoveTokenCommand(iouToken.amount.token, listOf(0), listOf(0)), iouToken.participants.map{it.owningKey})
            .addCommand(IOUTokenContract.Commands.Settle(), iouToken.participants.map{it.owningKey})

        proof.map{txBuilder.addAttachment(it)}

        txBuilder.verify(serviceHub)

        val donorSession = initiateFlow(iouToken.lender)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(donorSession)))

        return subFlow(FinalityFlow(stx, listOf(donorSession)))
    }
}