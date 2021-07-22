package com.template.states

import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

class IOUToken(val borrower : Party,
               val lender : Party,
               val amount : Amount<Int>,
               val timeWindow: TimeWindow,
                val participants: List<AbstractParty>,
                val linearId: UniqueIdentifier)



