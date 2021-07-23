package com.template.contracts

import com.template.states.IOUToken
import com.template.states.TokenAsset
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

class IOUTokenContract : Contract {

    companion object {
        @JvmStatic
        val IOUTOKEN_CONTRACT_ID = "com.template.contracts.IOUTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val inputs = tx.inputsOfType<IOUToken>()
        val outputs = tx.outputsOfType<IOUToken>()
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs states allowed when issuing an IOUToken state" using (inputs.isEmpty())
                "There must only one output state when issuing an IOUToken state" using (outputs.size == 1)

                val iouToken = outputs.single()
                "The amount on the iou must be positive" using (iouToken.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (iouToken.borrower != iouToken.lender)

                "Both lender and borrower have to sign the IOUTOken." using
                        (command.signers.toSet() == iouToken.participants.map { it.owningKey }.toSet())
            }

            is Commands.Settle -> requireThat{
                "There must be one input IOUToken state" using (inputs.size == 1)
                val iouToken = inputs.single()
                val outputTokens = tx.outputsOfType<TokenAsset>()
                "There must be at least one output TokenAsset" using (outputTokens.isNotEmpty())

                val validTokens = outputTokens.filter{it.holder == iouToken.lender && it.amount.token == iouToken.amount.token}
                "There must be at least one valid Token" using (validTokens.isNotEmpty())

                val sumTokens = validTokens.sumBy{it.amount.quantity.toInt()}
                "The amount of tokens should be the same" using (iouToken.amount.quantity.toInt() == sumTokens)
            }

        }
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Settle : Commands
    }
}