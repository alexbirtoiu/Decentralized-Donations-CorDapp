package com.template.contracts

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.sun.prism.impl.ps.CachingShapeRep
import com.template.states.IOUMoney
import com.template.states.IOUToken
import net.corda.core.contracts.*
import net.corda.core.contracts.Amount.Companion.sumOrZero
import net.corda.core.transactions.LedgerTransaction

class IOUMoneyContract : Contract {

    companion object {
        @JvmStatic
        val ID = "com.template.contracts.IOUMoneyContract"
    }

    private val currencies = listOf("GBP", "EUR", "USD").map{FiatCurrency.getInstance(it)}

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
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

            is Commands.Settle -> requireThat {
                val inputs = tx.inputsOfType<IOUMoney>()
                "There must exactly be one IOUMoney input state" using (inputs.size == 1)
                val iouMoney = inputs.single()

                val currency = iouMoney.amount.token
                "This currency is not allowed" using (currency in currencies)

                val cashInputs = tx.inputsOfType<FungibleToken>()
                val validCashInputs = cashInputs.filter{it.amount.token == currency && it.holder == iouMoney.borrower}
                "All money input states must be valid" using (cashInputs == validCashInputs)

                val cashOutputs = tx.outputsOfType<FungibleToken>()
                val validCashOutputs = cashOutputs.filter{it.amount.token == currency && it.holder == iouMoney.lender}
                "All money output states must be valid" using (cashOutputs == validCashOutputs)

                val sumCash = validCashOutputs.map{it.amount}.sumOrZero(currency)
                "Returned amount must be positive" using (sumCash.quantity > 0)
                "Returned amount on the iou must match the previously donated amount" using (sumCash == iouMoney.amount)

                "Both lender and borrower have to sign the issue of IOUMoney" using
                        (command.signers.toSet() == iouMoney.participants.map{ it.owningKey }.toSet())
            }

        }
    }

    interface Commands : CommandData {
        class Obligate : Commands
        class Settle : Commands
    }
}