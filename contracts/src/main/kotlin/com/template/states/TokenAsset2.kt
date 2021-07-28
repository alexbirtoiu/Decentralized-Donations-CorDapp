package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.template.contracts.TokenAsset2Contract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(TokenAsset2Contract::class)
class TokenAsset2(
    val token: FungibleToken,
    val proogs: List<SecureHash>,
    override val fractionDigits: Int = 0,
    override val maintainers: List<Party>, //= listOf(token.holder),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType() {
}