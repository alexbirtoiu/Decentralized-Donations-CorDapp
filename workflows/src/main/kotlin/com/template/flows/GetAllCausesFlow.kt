package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.Cause
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class GetAllCausesFlow : FlowLogic<Pair<List<Cause>, Map<String, String>>>() {

    @Suspendable
    override fun call(): Pair<List<Cause>, Map<String, String>> {
        val nodes = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first()}.minus(ourIdentity).toSet()
        val sessions = nodes.map{ initiateFlow(it) }
        val causes : MutableCollection<Cause> = mutableListOf()
        sessions.forEach{val list = it.receive<List<Cause>>().unwrap{it}; causes.addAll(list)}

        val reliability : MutableMap<String, String> = mutableMapOf()
        sessions.forEach{it ->
            val color = it.receive<String>().unwrap{it}
            reliability[it.counterparty.name.organisation] = color
        }

        return Pair(causes.toSet().toList(), reliability)
    }
}