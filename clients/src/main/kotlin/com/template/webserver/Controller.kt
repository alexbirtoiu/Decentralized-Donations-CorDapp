package com.template.webserver

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.flows.*
import com.template.states.Cause
import com.template.states.IOUMoney
import com.template.states.IOUToken
import com.template.states.RewardToken
import net.corda.core.contracts.Amount.Companion.sumOrZero
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
@CrossOrigin(origins = ["http://localhost:3000"]) // The paths for HTTP requests are relative to this base path.
class Controller() {
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

    private val tokenExpiryDuration = Duration.ofMinutes(1)

    private val currencyCodes = listOf("GBP", "EUR", "USD")

    private val currencies = currencyCodes.map{it to getCurrency(it)}

    private var reliability = mapOf("PartyA" to "green", "PartyB" to "green", "PartyC" to "green")

    private fun isNotary(nodeInfo: NodeInfo)
        = proxy.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }

    private fun isMe(nodeInfo: NodeInfo)
        = nodeInfo.legalIdentities.first().name.toString() == myName()

    private fun isNetworkMap(nodeInfo: NodeInfo)
        = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"

    private fun getParty(partyName : String)
        = proxy.networkMapSnapshot()
        .single { it.legalIdentities.first().name.organisation == partyName}
        .legalIdentities.first()

    private fun myName()
        = proxy.nodeInfo().legalIdentities.first().name.toString()

    private fun myParty()
        = proxy.nodeInfo().legalIdentities.first()

    private fun getCurrency(currencyCode : String)
        = FiatCurrency.getInstance(currencyCode)

    private fun toShow(x : Long)
        =   x.toDouble() / 100

    private fun getGroups(type : String): Map<UniqueIdentifier, List<IOUToken>> {
        val (iouTokens, update) = proxy.vaultTrack(IOUToken::class.java)

        return iouTokens.states.map { it.state.data }
            .filter { if (type == "owed")
                        it.borrower == myParty()
                    else
                        it.lender == myParty() }
            .groupBy { it.causeId }
    }

    private fun causesToJson(causes : List<Cause>) : MutableList<JsonObject> {
        val res : MutableList<JsonObject> = mutableListOf()
        causes.forEach{
            val obj = JsonObject()
            obj.addProperty("name", it.name)
            obj.addProperty("description", it.description)
            obj.addProperty("neededAmount", toShow(it.neededAmount.quantity))
            obj.addProperty("currency", it.neededAmount.token.tokenIdentifier)
            obj.addProperty("gatheredAmount", toShow(it.gatheredAmount.quantity))
            obj.addProperty("timeLimit", it.timeLimit.toString())
            obj.addProperty("linearId", it.linearId.toString())
            obj.addProperty("party", it.issuer.name.organisation)
            obj.addProperty("reliability", reliability[it.issuer.name.organisation])
            res.add(obj)
        }
        return res
    }

    private fun getCause(causeId : UniqueIdentifier) : Pair<Cause, Boolean> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(causeId))
        val cause = proxy.vaultQueryByCriteria(criteria, Cause::class.java).states

        if(cause.isNotEmpty())
            return Pair(cause.single().state.data, true)

        val consumedCriteria =
            QueryCriteria.LinearStateQueryCriteria(linearId = listOf(causeId), status = Vault.StateStatus.CONSUMED)

        val consumedCause = proxy.vaultQueryByCriteria(consumedCriteria, Cause::class.java).states
            .map { it.state.data }
            .single { it.gatheredAmount == it.neededAmount }

        return Pair(consumedCause, false)
    }

    private fun groupsToJson(groups : Map<UniqueIdentifier, List<IOUToken>>): String{
        val res : MutableList<JsonObject> = mutableListOf()
        groups.forEach {
            val obj = JsonObject()
            val tokenType = it.value.map{iouToken -> iouToken.amount.token}.toSet().single()
            val totalAmount = it.value.map{iouToken -> iouToken.amount}.sumOrZero(tokenType)

            val (cause, show) = getCause(it.key)

            obj.addProperty("causeId", cause.linearId.toString())
            obj.addProperty("name", cause.name)
            obj.addProperty("description", cause.description)
            obj.addProperty("expirationDate", it.value.first().expirationDate.toString())
            obj.addProperty("amount", toShow(totalAmount.quantity))
            obj.addProperty("token", totalAmount.token.tokenIdentifier)
            obj.addProperty("donatedAmount", toShow(cause.neededAmount.quantity))
            obj.addProperty("currency", cause.neededAmount.token.tokenIdentifier)
            obj.addProperty("show", show)

            res.add(obj)
        }
        return res.toString()
    }

    private fun incomingTokensToJson(groups : Map<UniqueIdentifier, List<IOUToken>>) : String{
        val res : MutableList<JsonObject> = mutableListOf()
        groups.forEach {
            val obj = JsonObject()
            val tokenType = it.value.map{iouToken -> iouToken.amount.token}.toSet().single()
            val totalAmount = it.value.map{iouToken -> iouToken.amount}.sumOrZero(tokenType)

            val currency = it.value.map{iouToken -> iouToken.donated.token}.toSet().single()
            val donatedAmount = it.value.map{iouToken -> iouToken.donated}.sumOrZero(currency)

            obj.addProperty("causeId", it.key.toString())
            obj.addProperty("expirationDate", it.value.first().expirationDate.toString())
            obj.addProperty("amount", toShow(totalAmount.quantity))
            obj.addProperty("token", totalAmount.token.tokenIdentifier)
            obj.addProperty("donatedAmount", toShow(donatedAmount.quantity))
            obj.addProperty("currency", donatedAmount.token.tokenIdentifier)

            res.add(obj)
        }
        return res.toString()
    }

    private fun getImgFromHash(hash : String) : String {
        val zipStream = ZipInputStream(proxy.openAttachment(SecureHash.parse(hash)))
        val entry = zipStream.nextEntry

        while (zipStream.available() > 0)
            zipStream.read()
        return entry.name
    }

    private fun tokensToJson(tokens : List<RewardToken>) : String {
        val res : MutableList<JsonObject> = mutableListOf()
        tokens.forEach {
            val obj = JsonObject()
            obj.addProperty("amount", toShow(it.amount.quantity))
            obj.addProperty("token", it.amount.token.tokenIdentifier + " from " + it.amount.token.issuer.name.organisation)
            obj.addProperty("image", getImgFromHash(it.proof.single().toString()))

            res.add(obj)
        }
        return res.toString()
    }

    private fun iouMoneyToJson(iouMoney: List<IOUMoney>) : String {
        val res : MutableList<JsonObject> = mutableListOf()
        iouMoney.forEach {
            val obj = JsonObject()

            obj.addProperty("amount", toShow(it.amount.quantity))
            obj.addProperty("currency", it.amount.token.tokenIdentifier)
            obj.addProperty("lender", it.lender.name.organisation)
            obj.addProperty("linearId", it.linearId.toString())

            res.add(obj)
        }
        return res.toString()
    }

    @GetMapping(value = ["/node"])
    private fun getNode(): String {
        return proxy.nodeInfo().legalIdentities.first().name.organisation
    }

    @GetMapping(value = ["/peers"])
    private fun getPeers(): List<String> {
        return proxy.networkMapSnapshot()
            .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
            .map { it.legalIdentities.first().name.organisation }
            .sortedBy { it }
    }

    @GetMapping(value = ["/money"], produces = [ APPLICATION_JSON_VALUE ])
    private fun updateMoney(): ResponseEntity<String> {
        val moneyStates = proxy.vaultQuery(FungibleToken::class.java).states
            .map { it.state.data }
            .filter { it.amount.token.issuer == getParty("Bank") }
            .map { it.amount.withoutIssuer() }

        val json = JsonObject()
        currencies.map{json.addProperty(it.first, toShow(moneyStates.filter{state -> state.token == it.second}.sumOrZero(it.second).quantity))}

        return ResponseEntity.ok(json.toString())
    }

    // Get Mappings

    @GetMapping(value = ["/owedtokens"], produces = [ APPLICATION_JSON_VALUE ])
    private fun owedTokens() : ResponseEntity<String> {
        val groups = getGroups("owed")

        return ResponseEntity.ok(groupsToJson(groups))
    }

    @GetMapping(value = ["/incomingtokens"], produces = [ APPLICATION_JSON_VALUE ])
    private fun incomingTokens() : ResponseEntity<String> {
        val groups = getGroups("incoming")

        return ResponseEntity.ok(incomingTokensToJson(groups))
    }

    @GetMapping(value = ["/ioumoney"], produces = [ APPLICATION_JSON_VALUE ])
    private fun iouMoney() : ResponseEntity<String> {
        val iouMoney = proxy.vaultQuery(IOUMoney::class.java).states
            .map{it.state.data}
            .filter{it.borrower == myParty()}

        return ResponseEntity.ok(iouMoneyToJson(iouMoney))
    }

    @GetMapping(value = ["/mycauses"], produces = [ APPLICATION_JSON_VALUE ])
    private fun myCauses() : ResponseEntity<String> {
        val causes = proxy.vaultQuery(Cause::class.java).states.map{it.state.data}

        return ResponseEntity.ok(causesToJson(causes).toString())

    }

    @GetMapping(value = ["/activecauses"], produces = [ APPLICATION_JSON_VALUE ])
    private fun activeCauses() : ResponseEntity<String> {
        val causes = proxy.startFlowDynamic(
            GetAllCausesFlow::class.java
        ).returnValue.get()

        reliability = causes.second

        return ResponseEntity.ok(causesToJson(causes.first).toString())
    }

    @GetMapping(value = ["/rewardtokens"], produces = [ APPLICATION_JSON_VALUE ])
    private fun rewardTokens() : ResponseEntity<String>{
        val rewardTokens = proxy.vaultQuery(RewardToken::class.java).states.map{it.state.data}

        return ResponseEntity.ok(tokensToJson(rewardTokens))
    }

    // POST Mappings

    @RequestMapping(value = ["/party"])
    private fun getParty(@RequestBody payload: String?): ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val party = convertedObject["name"].asString
        when (party) {
            "PartyA"-> proxy = partyAProxy
            "PartyB"-> proxy = partyBProxy
            "PartyC"-> proxy = partyCProxy
            "Bank"  -> proxy = bankProxy
            else -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unrecognised Party")
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Party changed successfully to " + myName())
    }

    @RequestMapping(value = ["/issuemoney"])
    private fun issueMoney(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val amount = convertedObject["amount"].asLong
        val currency = getCurrency(convertedObject["currency"].asString)
        val party = getParty(convertedObject["party"].asString)
        try {
            proxy.startFlowDynamic(
                IssueFungibleTokenFlow::class.java,
                amount of currency,
                party
            ).returnValue.getOrThrow()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Money has been issued")
    }

    @RequestMapping(value = ["/issuecause"])
    private fun issueCause(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val name = convertedObject["name"].asString
        val description = convertedObject["description"].asString

        val neededAmount = convertedObject["neededAmount"].asLong
        val currency = getCurrency(convertedObject["currency"].asString)
        val issuedNeededAmount = neededAmount of currency issuedBy getParty("Bank")

        val totalTokens = convertedObject["totalTokens"].asLong
        val tokenName = convertedObject["tokenName"].asString
        val totalTokensAmount = totalTokens of TokenType(tokenName, 2) issuedBy myParty()

        val deadLine = convertedObject["timeLimit"].asString
        val timeLimit = LocalDateTime.parse(deadLine, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val cause = Cause(name, description, issuedNeededAmount, totalTokensAmount, timeLimit, tokenExpiryDuration)
         try {
             val causeId = proxy.startFlowDynamic(
                 IssueCauseFlow::class.java,
                 cause
             ).returnValue.getOrThrow().tx.outputsOfType<Cause>().single().linearId
             return ResponseEntity.status(HttpStatus.CREATED).body("Cause has been issued with id: $causeId")
         } catch (e : Exception) {
             print(e.message)
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
         }

    }

    @RequestMapping(value = ["/donate"])
    private fun donate(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val amount = convertedObject["amount"].asLong

        val causeObject = convertedObject["cause"] as JsonObject
        val party = getParty(causeObject["party"].asString)
        val causeId = fromString(causeObject["linearId"].asString)
        val currency = getCurrency(causeObject["currency"].asString)

        try {
            val ptx = proxy.startFlowDynamic(
                DonateFlow::class.java,
                party,
                amount of currency issuedBy getParty("Bank"),
                causeId
            ).returnValue.getOrThrow()
            println("owed" + ptx.tx.outputsOfType<IOUToken>().single().amount)
            return ResponseEntity.status(HttpStatus.CREATED).body("Money has been donated to the cause with id: $causeId")
        } catch (e : Exception) {
            print(e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @RequestMapping(value = ["/settlecause"])
    private fun settleCause(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val causeId = fromString(convertedObject["linearId"].asString)
        try {
            proxy.startFlowDynamic(
                SettleCauseFlow::class.java,
                causeId
            ).returnValue.getOrThrow()
            return ResponseEntity.status(HttpStatus.CREATED).body("Cause with id $causeId has been settled and the money has been redeemed")
        } catch (e : Exception) {
            print(e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @RequestMapping(value = ["/requestmoney"])
    private fun requestMoney(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val causeId = fromString(convertedObject["linearId"].asString)

        getGroups("incoming").filter { it.key == causeId }.map{it.value}.single().forEach {
            try {
                proxy.startFlowDynamic(
                    IOUMoneyIssueFlow::class.java,
                    it.linearId
                ).returnValue.getOrThrow()

            } catch (e: Exception) {
                print(e.message)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("IOUMoney state has been issued")

    }

    @RequestMapping(value = ["/repay"])
    private fun repay(@RequestBody payload: String?) : ResponseEntity<String> {
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val linearId = fromString(convertedObject["linearId"].asString)

        try {
            proxy.startFlowDynamic(
                IOUMoneySettleFlow::class.java,
                linearId
            ).returnValue.getOrThrow()

        } catch (e: Exception) {
            print(e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Money has been returned successfully")

    }

    @PostMapping(value = ["/settletokens"])
    private fun settleTokens(@RequestParam("causeId") causeId : String,
                              @RequestParam("file") file: MultipartFile) : ResponseEntity<String> {

        val attachmentId = uploadFile(file)
        val proof = listOf(attachmentId)

        getGroups("owed").filter { it.key == fromString(causeId) }.map{it.value}.single().forEach {
            try {
                proxy.startFlowDynamic(
                    IOUTokenSettleFlow::class.java,
                    proof,
                    it.linearId
                    ).returnValue.getOrThrow()

            } catch (e: Exception) {
                print(e.message)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("All IOUTokens settled successfully")

    }

    private fun uploadFile(file : MultipartFile) : SecureHash{
        val filename = file.originalFilename
        require(filename != null) { "File name must be set" }
        val hash = if (!(file.contentType == "zip" || file.contentType == "jar")) {
            uploadZip(file.inputStream, filename)
        } else {
            proxy.uploadAttachmentWithMetadata(
                jar = file.inputStream,
                uploader = myName(),
                filename = filename)
        }
        return hash
    }

    private fun uploadZip(inputStream: InputStream, filename: String): AttachmentId {
        val zipName = "$filename-${UUID.randomUUID()}.zip"
        FileOutputStream(zipName).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                val zipEntry = ZipEntry(filename)
                zipOutputStream.putNextEntry(zipEntry)
                inputStream.copyTo(zipOutputStream, 1024)
            }
        }
        return FileInputStream(zipName).use { fileInputStream ->
            val hash = proxy.uploadAttachmentWithMetadata(
                jar = fileInputStream,
                uploader = myName(),
                filename = filename)
            try {
                Files.deleteIfExists(Paths.get(zipName))
            }catch (e : Exception) {
                println(e.message)
            }
            hash
        }
    }

}