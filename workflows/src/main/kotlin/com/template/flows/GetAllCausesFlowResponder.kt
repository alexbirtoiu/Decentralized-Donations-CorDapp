package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.Cause
import com.template.states.IOUMoney
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

@InitiatedBy(GetAllCausesFlow::class)
@StartableByRPC
class GetAllCausesFlowResponder(
    val counterpartySession : FlowSession
) : FlowLogic<Boolean>(){

    @Suspendable
    override fun call(): Boolean {
        val causeStates = serviceHub.vaultService.queryBy(Cause::class.java).states.map{it.state.data}
        counterpartySession.send(causeStates)

        val unconsumedCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val iouMoneyStates = serviceHub.vaultService
            .queryBy(IOUMoney::class.java, unconsumedCriteria).states
        if(iouMoneyStates.isNotEmpty())
            counterpartySession.send("red")

        val consumedCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.CONSUMED)
        val consumedIOUmoneyStates = serviceHub.vaultService
            .queryBy(IOUMoney::class.java, consumedCriteria).states
        if(consumedIOUmoneyStates.isNotEmpty())
            counterpartySession.send("orange")

        counterpartySession.send("green")
        return true;
    }
}