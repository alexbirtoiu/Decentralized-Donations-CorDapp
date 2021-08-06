package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.Create
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.DigitalCurrency
import com.r3.corda.lib.tokens.money.*
import com.template.states.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.finance.`issued by`
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test


class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.template", "com.r3.corda.lib.tokens.contracts"))
    var donor = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var organization = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))
    var bank = TestIdentity(CordaX500Name("Bank", "TestLand", "US"))
    private val tokenType = TokenType("proj", 0)
    val cause = Cause("Project", "description", 200.GBP issuedBy bank.party, 40 of tokenType issuedBy organization.party, 0.GBP issuedBy bank.party)

    @Test
    fun moneyTests() {
        val cash = 10.GBP issuedBy bank.party heldBy donor.party
        val movedCash = cash.withNewHolder(organization.party)
        val cash2 = 5.GBP issuedBy bank.party heldBy organization.party
        val cash3 = 5.USD issuedBy bank.party heldBy donor.party
        val movedCash2 = cash2.withNewHolder(organization.party)
        DigitalCurrency


        ledgerServices.ledger {
            transaction {
                output(FungibleTokenContract.contractId, cash)
                command(listOf(bank.party.owningKey, donor.party.owningKey), IssueTokenCommand(GBP issuedBy bank.party, listOf(0)))
                verifies()
            }

            transaction {
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash2)
                output(FungibleTokenContract.contractId, cash3)
                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(GBP issuedBy bank.party, listOf(0), listOf(0,1)))
                verifies()
            }

            transaction {
                input(FungibleTokenContract.contractId, movedCash)
                command(listOf(organization.party.owningKey, bank.party.owningKey), RedeemTokenCommand(GBP issuedBy bank.party, listOf(0)))
                verifies()
            }
        }

    }

    @Test
    fun donateTests() {
        val cashIn = 40.GBP issuedBy bank.party heldBy donor.party
        val cashOut = cashIn.withNewHolder(organization.party)
        val iouOut = cause.getIOUToken(donor.party, cashIn.amount)
        val modifiedCause = cause.receive(cashIn.amount)

        ledgerServices.ledger {
            transaction {
                input(FungibleTokenContract.contractId, cashIn)
                output(FungibleTokenContract.contractId, cashOut)
                input(CauseContract.ID, cause)
                output(CauseContract.ID, modifiedCause)
                output(IOUTokenContract.ID, iouOut)
                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(GBP issuedBy bank.party, listOf(0), listOf(0)))
                command(listOf(organization.party.owningKey), CauseContract.Commands.Donate())
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUTokenContract.Commands.Donate())
                verifies()
            }
        }
    }

    @Test
    fun causeTests() {

        ledgerServices.ledger {
            transaction {
                output(CauseContract.ID, cause)
                command(listOf(organization.party.owningKey), CauseContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun iouTokenTests() {
        val iouToken = cause.getIOUToken(donor.party, 35.GBP issuedBy bank.party)//IOUToken(donor.party, null, 5 of tokenType issuedBy organization.party, 10.GBP issuedBy bank.party, UniqueIdentifier())

        println(iouToken.amount.quantity)
        val cash1 = 32.GBP issuedBy bank.party heldBy donor.party
        val cash2 = 3.GBP issuedBy bank.party heldBy donor.party
        val movedCash1 = cash1.withNewHolder(organization.party)
        val movedCash2 = cash2.withNewHolder(organization.party)

        val token1 = 3 of tokenType issuedBy organization.party heldBy organization.party
        val token2 = 4 of tokenType issuedBy organization.party heldBy organization.party
        val movedToken1 = token1.withNewHolder(donor.party)
        val movedToken2 = token2.withNewHolder(donor.party)

        // Donate

        ledgerServices.ledger {
            transaction {
                input(FungibleTokenContract.contractId, cash1)
                input(FungibleTokenContract.contractId, cash2)
                output(FungibleTokenContract.contractId, movedCash1)
                output(FungibleTokenContract.contractId, movedCash2)
                output(IOUTokenContract.ID, iouToken)
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUTokenContract.Commands.Donate())
                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(GBP issuedBy bank.party, listOf(0,1), listOf(0,1)))
                verifies()
            }
        }

        //Settle

        ledgerServices.ledger {
            transaction {
                input(FungibleTokenContract.contractId, token1)
                input(FungibleTokenContract.contractId, token2)
                input(IOUTokenContract.ID, iouToken)
                output(FungibleTokenContract.contractId, movedToken1)
                output(FungibleTokenContract.contractId, movedToken2)
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUTokenContract.Commands.Settle())
                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(tokenType issuedBy organization.party, listOf(0,1), listOf(0,1)))
                verifies()
            }
        }

    }


    @Test
    fun tokenTests() {

    }

    @Test
    fun iouMoneyTests() {
        val iouToken = IOUToken(donor.party, null, 5 of tokenType issuedBy organization.party, 10.GBP issuedBy bank.party, UniqueIdentifier())
        val iouMoney = IOUMoney(organization.party, donor.party, 10.GBP issuedBy bank.party)

        val cash1 = 7.GBP issuedBy bank.party heldBy organization.party
        val cash2 = 3.GBP issuedBy bank.party heldBy organization.party
        val movedCash1 = cash1.withNewHolder(donor.party)
        val movedCash2 = cash2.withNewHolder(donor.party)

        //Settle

        ledgerServices.ledger {
            transaction {
                input(FungibleTokenContract.contractId, cash1)
                input(FungibleTokenContract.contractId, cash2)
                output(FungibleTokenContract.contractId, movedCash1)
                output(FungibleTokenContract.contractId, movedCash2)
                input(IOUMoneyContract.ID, iouMoney)
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUMoneyContract.Commands.Settle())
                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(GBP issuedBy bank.party, listOf(0,1), listOf(0,1)))
                verifies()
            }
        }

        //Obligate

        ledgerServices.ledger {
            transaction {
                input(IOUTokenContract.ID, iouToken)
                output(IOUMoneyContract.ID, iouMoney)
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUTokenContract.Commands.Obligate())
                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUMoneyContract.Commands.Obligate())
                verifies()
            }
        }
    }

    @Test
    fun usecaseTests() {

    }

    @Test
    fun dummytest() {
//        val iouMoneyInput = IOUMoney(organization.party, donor.party, 10.POUNDS)
//
//
//        val tokenType = TokenType("ORG", 0)
//        val issuedTokenType = IssuedTokenType(organization.party, tokenType)
//
//        val tokenAsset = TokenAsset( null, Amount(11, issuedTokenType), organization.party)
//        val movedTokenAsset = tokenAsset.withNewHolder(donor.party)
//
//        val iouTokenInput = IOUToken(donor.party, null, Amount(10, issuedTokenType), 10.POUNDS)
//
//       // val fungibleToken = 1 of tokenType issuedBy organization.party heldBy organization.party

        ledgerServices.ledger {

//            transaction {
//                output(FungibleTokenContract.contractId, tokenAsset)
//                command(listOf(organization.party.owningKey), IssueTokenCommand(issuedTokenType, listOf(0)))
//                verifies()
//            }

//            transaction {
//                input(IOUTokenContract.ID, iouTokenInput)
//                input(FungibleTokenContract.contractId, tokenAsset)
//                output(FungibleTokenContract.contractId, movedTokenAsset)
//                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUTokenContract.Commands.Settle())
//                command(listOf(donor.party.owningKey, organization.party.owningKey), MoveTokenCommand(issuedTokenType,listOf(1),listOf(0)))
//                fails()
//            }

//            transaction {
//                input(IOUMoneyContract.ID, iouMoneyInput)
//                input(Cash.PROGRAM_ID, cashInput)
//                output(Cash.PROGRAM_ID, cashOutput)
//                command(listOf(donor.party.owningKey, organization.party.owningKey), IOUMoneyContract.Commands.Settle())
//                command(listOf(donor.party.owningKey, organization.party.owningKey, bank.party.owningKey), Cash.Commands.Move())
//                verifies()
//            }

//            transaction {
//                input(IOUMoneyContract.ID, iouTokenInput)
//                output(IOUMoneyContract.ID, iouMoneyOutput)
//                command(listOf(organization.party.owningKey, donor.party.owningKey, bank.party.owningKey), IOUMoneyContract.Commands.Issue())
//                verifies()
//            }
        }
    }
}