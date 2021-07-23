package com.template.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class IOUMoneyContract : Contract {

    companion object {
        @JvmStatic
        val IOUTOKEN_CONTRACT_ID = "com.template.contracts.IOUMoneyContract"
    }

    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }
}