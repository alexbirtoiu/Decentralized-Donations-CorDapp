package com.template.states

import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.IOUTokenContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*


@BelongsToContract(IOUTokenContract::class)
class IOUToken(
    val lender : Party,
    val timeWindow : TimeWindow ?= null,
    val amount : Amount<IssuedTokenType>,
    val donated : Amount<IssuedTokenType>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
   ) : LinearState, SchedulableState {

    val borrower = amount.token.issuer

    override val participants = listOf(borrower, lender)

    override fun nextScheduledActivity(
        thisStateRef: StateRef,
        flowLogicRefFactory: FlowLogicRefFactory
    ): ScheduledActivity? {
        TODO("Not yet implemented")
    }
}



