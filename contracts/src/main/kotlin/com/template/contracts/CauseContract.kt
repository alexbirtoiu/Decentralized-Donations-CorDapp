package com.template.contracts

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.states.Cause
import net.corda.core.contracts.*
import net.corda.core.contracts.Amount.Companion.sumOrZero
import net.corda.core.transactions.LedgerTransaction
import java.util.*

class CauseContract : Contract {

    companion object {
        @JvmStatic
        val ID = "com.template.contracts.CauseContract"
    }

    private val currencies = listOf("GBP", "EUR", "USD").map{ FiatCurrency.getInstance(it)}

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when(command.value) {
            is Commands.Issue -> requireThat {
                "There must be no inputs when issuing a Cause state" using (tx.inputs.isEmpty())
                val outputs = tx.outputsOfType<Cause>()
                "There must be only one output Cause state" using (outputs.size == 1)
                val cause = outputs.single()

                "The amount of rewarding tokens must be positive" using (cause.totalTokens.quantity > 0)

                "The currency must be available" using (cause.currency.tokenType in currencies)
                "The needed amount on the cause must be positive" using (cause.neededAmount.quantity > 0)

                "The initial gatheredAmount must be 0" using (cause.gatheredAmount.quantity.toInt() == 0)
                "The gatheredAmount's currency must be the same" using (cause.gatheredAmount.token == cause.currency)

                "The name must not be empty" using (cause.name.isNotEmpty())
                "The description must not be empty" using (cause.description.isNotEmpty())

                "The issuer of the cause must be a signer" using (command.signers.toSet() == cause.participants.map{it.owningKey}.toSet())
            }
            is Commands.Donate -> requireThat {
                val inputs = tx.inputsOfType<Cause>()
                "There must be only one input Cause state" using (inputs.size == 1)
                val inputCause = inputs.single()

                val outputs = tx.outputsOfType<Cause>()
                "There must be only one output Cause state" using (outputs.size == 1)
                val outputCause = outputs.single()

                val currency = inputCause.currency

                val cashOutputs = tx.outputsOfType<FungibleToken>()
                val validCashOutputs = cashOutputs.filter{it.amount.token == currency && it.holder == inputCause.issuer}
                "There must be valid cash outputs" using (validCashOutputs.isNotEmpty())

                val sumCash = validCashOutputs.map{it.amount}.sumOrZero(currency)
                "Donated amount must be positive" using (sumCash.quantity > 0)

                "Everything except the gathered amount must be the same" using (inputCause.receive(sumCash) == outputCause)

                "The amount gathered plus the new donated amount must not exceed the total needed amount" using (outputCause.gatheredAmount <= outputCause.neededAmount)

                "The issuer of the Cause state must sign the payment" using (command.signers.toSet() == inputCause.participants.map{it.owningKey}.toSet())
            }
            is Commands.Settle -> requireThat {
                val inputs = tx.inputsOfType<Cause>()
                "There must be only one input Cause state" using (inputs.size == 1)
                val cause = inputs.single()

                val outputCauses = tx.outputsOfType<Cause>()
                "There must be no output Cause states" using (outputCauses.isEmpty())

                val currency = cause.currency

                "The gathered amount on the Cause state must match the nedeed amount" using
                        (cause.neededAmount == cause.gatheredAmount)

                val cashInputs = tx.inputsOfType<FungibleToken>()
                val validCashInputs = cashInputs.filter{it.amount.token == currency && it.holder == cause.issuer}
                "There must be valid input Cash states" using (validCashInputs.isNotEmpty())

                val sumCash = validCashInputs.map{it.amount}.sumOrZero(currency)
                "The sum of cash input states must match the nedded amount on the Cause state" using
                        (sumCash == cause.neededAmount)

                "The cause issuer must sign the transaction" using
                        (command.signers.toSet() == cause.participants.map{it.owningKey}.toSet())
            }

        }

    }

    interface Commands : CommandData {
        class Issue : Commands
        class Donate : Commands
        class Settle : Commands
    }
}