package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.transactions.LedgerTransaction

class TokenAsset2Contract : EvolvableTokenContract() {

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }
}