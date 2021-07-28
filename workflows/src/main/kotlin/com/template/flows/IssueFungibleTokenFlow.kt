package com.template.flows

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class IssueFungibleTokenFlow(
    val amount: Amount<TokenType>,
    val holder : AbstractParty
) : FlowLogic<SignedTransaction>(){

    override fun call(): SignedTransaction {
        val fungibleToken = amount issuedBy ourIdentity heldBy holder

        return subFlow(IssueTokens(listOf(fungibleToken)))
    }
}