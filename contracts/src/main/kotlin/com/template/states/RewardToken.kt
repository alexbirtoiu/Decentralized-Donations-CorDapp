package com.template.states

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.AttachmentId

@BelongsToContract(FungibleTokenContract::class)
class RewardToken(
    val proof : List<AttachmentId>,
    val fungibleToken: FungibleToken
) : FungibleToken(fungibleToken.amount, fungibleToken.holder) {

    override fun withNewHolder(newHolder: AbstractParty): RewardToken {
        return RewardToken(proof, fungibleToken.withNewHolder(newHolder))
    }
}