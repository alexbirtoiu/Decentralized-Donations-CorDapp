package com.template.webserver

import net.corda.core.messaging.CordaRPCOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.template.states.IOUToken
import net.corda.core.contracts.Amount.Companion.sumOrZero
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
@CrossOrigin(origins = ["http://localhost:3000"]) // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    @Autowired
    lateinit var partyAProxy: CordaRPCOps

    @Autowired
    lateinit var partyBProxy: CordaRPCOps

    @Autowired
    lateinit var partyCProxy: CordaRPCOps

    @Autowired
    lateinit var bankProxy: CordaRPCOps

    @Autowired
    @Qualifier("partyAProxy")
    lateinit var proxy: CordaRPCOps

    val tokenType = TokenType("GBP", 0)

    fun myName() : String {
        return proxy.nodeInfo().legalIdentities.first().name.toString()
    }

    @GetMapping(value = ["/templateendpoint"], produces = ["text/plain"])
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @RequestMapping(value = ["/party"])
    private fun getParty(@RequestBody payload: String?): ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val party = convertedObject["name"].asString
        when (party) {
            "PartyA"-> proxy = partyAProxy
            "PartyB"-> proxy = partyBProxy
            "PartyC"-> proxy = partyCProxy
            "Bank"  -> proxy = bankProxy
            else -> return ResponseEntity.status(HttpStatus.CREATED).body("Unrecognised Party")
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Party changed successfully to " + myName())
    }

    @GetMapping(value = ["/money"], produces = ["text/plain"])
    private fun updateMoney(): String {
        val moneyState = proxy.vaultQuery(FungibleToken::class.java).states
            .map { it.state.data }
            .map { it.amount.withoutIssuer() }
        println(moneyState.sumOrZero(tokenType).quantity.toString())
        return moneyState.sumOrZero(tokenType).quantity.toString()
    }
}