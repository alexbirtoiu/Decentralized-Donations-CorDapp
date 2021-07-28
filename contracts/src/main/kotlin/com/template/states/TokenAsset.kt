package com.template.states

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType

@BelongsToContract(FungibleTokenContract::class)
class TokenAsset(
    override val amount: Amount<IssuedTokenType>,
    override val holder: AbstractParty,
    val proofs: List<SecureHash>?
    ) : FungibleToken(amount, holder) {

    override fun withNewHolder(newHolder: AbstractParty): TokenAsset {
        return TokenAsset( amount, newHolder, proofs)
    }
}