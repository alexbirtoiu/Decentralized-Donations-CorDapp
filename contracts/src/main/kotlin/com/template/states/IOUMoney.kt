package com.template.states

import com.template.contracts.IOUMoneyContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(IOUMoneyContract::class)
class IOUMoney (
    val borrower : Party,
    val lender : Party,
    val amount : Amount<Currency>,
    override val linearId: UniqueIdentifier
) : LinearState{
        override val participants = listOf(borrower, lender)
    }