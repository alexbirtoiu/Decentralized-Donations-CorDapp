package com.template

import com.r3.corda.lib.tokens.money.GBP
import com.template.flows.IssueFungibleTokenFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 3) { "Usage: Client <node address> <rpc username> <rpc password>" }
        val nodeAddress = parse(args[0])
        val rpcUsername = args[1]
        val rpcPassword = args[2]
        val client = CordaRPCClient(nodeAddress)
        val clientConnection = client.start(rpcUsername, rpcPassword)
        val proxy = clientConnection.proxy

        proxy.startFlowDynamic(
            IssueFungibleTokenFlow::class.java,
            100.GBP,
            proxy.nodeInfo().legalIdentities.first()
        )
        clientConnection.close()
    }
}