package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class RedeemMoneyFlow(
    val amount : Amount<IssuedTokenType>
    ) : FlowLogic<SignedTransaction>() {

    private val currencies = listOf("GBP", "EUR", "USD").map{ FiatCurrency.getInstance(it) }

    @Suspendable
    override fun call(): SignedTransaction {
        if (amount.token.tokenType !in currencies)
            throw FlowException("Currency not allowed")

        return subFlow(RedeemFungibleTokens(amount.withoutIssuer(), amount.token.issuer))
    }
}