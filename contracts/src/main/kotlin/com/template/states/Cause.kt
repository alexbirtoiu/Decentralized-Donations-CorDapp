package com.template.states

import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.template.contracts.CauseContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(CauseContract::class)
class Cause (
    val name : String,
    val description : String,
    val neededAmount : Amount<IssuedTokenType>,
    val totalTokens : Amount<IssuedTokenType>,
    val gatheredAmount : Amount<IssuedTokenType>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    val currency = neededAmount.token
    val issuer = totalTokens.token.issuer
    override val participants: List<AbstractParty> = listOf(issuer)

    fun getIOUToken(donor : Party, donatedAmount : Amount<IssuedTokenType>) : IOUToken {
        if(donatedAmount.quantity > neededAmount.quantity)
            throw Exception("Donated Amount cannot be higher than neededAmount")

        val requiredTokenQuantity = (donatedAmount.quantity * totalTokens.quantity) / neededAmount.quantity
        val rewardedTokens = Amount(requiredTokenQuantity, totalTokens.token)

        return IOUToken(donor, null, rewardedTokens, donatedAmount, linearId)
    }

    fun receive(donatedAmount: Amount<IssuedTokenType>) = Cause(name, description, neededAmount, totalTokens, gatheredAmount.plus(donatedAmount), linearId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cause

        if (name != other.name) return false
        if (description != other.description) return false
        if (neededAmount != other.neededAmount) return false
        if (totalTokens != other.totalTokens) return false
        if (gatheredAmount != other.gatheredAmount) return false
        if (linearId != other.linearId) return false
        if (currency != other.currency) return false
        if (issuer != other.issuer) return false
        if (participants != other.participants) return false

        return true
    }

}