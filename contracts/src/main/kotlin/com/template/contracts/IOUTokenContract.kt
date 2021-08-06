package com.template.contracts

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.states.Cause
import com.template.states.IOUMoney
import com.template.states.IOUToken
import com.template.states.TokenAsset
import net.corda.core.contracts.Amount.Companion.sumOrZero
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat
import sun.rmi.runtime.Log
import java.util.*

class IOUTokenContract : Contract {

    companion object {
        @JvmStatic
        val ID = "com.template.contracts.IOUTokenContract"
    }

    private val currencies = listOf("GBP", "EUR", "USD").map{FiatCurrency.getInstance(it)}

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Donate -> requireThat {
                val outputs = tx.outputsOfType<IOUToken>()
                "There must be exactly one IOUToken output state" using (outputs.size == 1)
                val iouToken = outputs.single()
                "The amount on the iou must be positive" using (iouToken.amount.quantity > 0)

                val causeOutputs = tx.outputsOfType<Cause>()
                "There must be only one Cause output state" using (causeOutputs.size == 1)
                val cause = causeOutputs.single()
                "The cause ID must match the IOUToken's cause id" using (cause.linearId == iouToken.causeId)

                val currency = iouToken.donated.token
                "This currency is not allowed" using (currency.tokenType in currencies)

                val cashOutputs = tx.outputsOfType<FungibleToken>()
                val validCashOutputs = cashOutputs.filter{it.amount.token == currency && it.holder == iouToken.borrower}
                "There must be valid cash outputs" using (validCashOutputs.isNotEmpty())

                val sumCash = validCashOutputs.map{it.amount}.sumOrZero(currency)
                "Donated amount must be positive" using (sumCash.quantity > 0)

                "Donated amount on the iou must match the actual donated amount" using (sumCash == iouToken.donated)

                "The lender and borrower cannot have the same identity." using (iouToken.borrower != iouToken.lender)

                "Both lender and borrower have to sign the IOUTOken." using
                        (command.signers.toSet() == iouToken.participants.map { it.owningKey }.toSet())
            }

            is Commands.Settle -> requireThat{
                val inputs = tx.inputsOfType<IOUToken>()
                "There must be exactly one IOUToken input state" using (inputs.size == 1)
                val iouToken = inputs.single()

                val expectedToken = iouToken.amount.token

//                val inputTokens = tx.inputsOfType<FungibleToken>()
//                val validInputTokens = inputTokens.filter{it.amount.token == expectedToken && it.holder == iouToken.borrower}
//                "All token input states must be valid" using (inputTokens == validInputTokens)

                val outputTokens = tx.outputsOfType<FungibleToken>()
                val validOutputTokens = outputTokens.filter{it.amount.token == expectedToken && it.holder == iouToken.lender}
                "There must be valid output Tokens" using (validOutputTokens.isNotEmpty())

                val sumTokens = validOutputTokens.map{it.amount}.sumOrZero(expectedToken)
                "The amount of tokens should be the same" using (iouToken.amount == sumTokens)

                "Both lender and borrower have to sign the settle transaction" using
                        (command.signers.toSet() == iouToken.participants.map{ it.owningKey }.toSet())
            }

            is Commands.Obligate -> requireThat {
                val outputs = tx.outputsOfType<IOUMoney>()
                "There must be exactly one IOUMoney output state" using (outputs.size == 1)
                val iouMoney = outputs.single()
                "The amount on the IOUMoney must be positive" using (iouMoney.amount.quantity > 0)


                val iouTokenInputs = tx.inputsOfType<IOUToken>()
                "There must be exactly one IOUToken input state" using (iouTokenInputs.size == 1)
                val iouToken = iouTokenInputs.single()


                "The lender and the borrower must be two different parties" using(iouMoney.lender != iouMoney.borrower)
                "The same amount of money donated must be returned to the donor" using (iouToken.donated == iouMoney.amount)

                "The lender on the both of the IOUs must be the same" using (iouToken.lender == iouMoney.lender)
                "The borrower on the both of the IOUs must be the same" using (iouToken.borrower == iouMoney.borrower)

                "Both lender and borrower have to sign the issue of IOUMoney" using
                        (command.signers.toSet() == iouToken.participants.map{ it.owningKey }.toSet())
            }
        }
    }

    interface Commands : CommandData {
        class Donate : Commands
        class Settle : Commands
        class Obligate : Commands
    }
}