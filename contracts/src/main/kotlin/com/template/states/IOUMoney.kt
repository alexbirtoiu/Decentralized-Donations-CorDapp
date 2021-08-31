package com.template.states

import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.IOUMoneyContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(IOUMoneyContract::class)
class IOUMoney (
    val borrower : Party,
    val lender : Party,
    val amount : Amount<IssuedTokenType>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState{
        override val participants = listOf(borrower, lender)
}