package com.template.states

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.security.PublicKey
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType

class TokenAsset(
    val proofs : List<SecureHash>,
    override val issuer : Party,
    override val amount : Amount<IssuedTokenType>,
    override val holder: AbstractParty,
    private val linearId: UniqueIdentifier
) : FungibleToken(amount, holder){



}