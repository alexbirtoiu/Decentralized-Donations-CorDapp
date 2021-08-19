package com.template.webserver

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class AppConfig : WebMvcConfigurer {

    val partyAHostAndPort = "localhost:10006"

    val partyBHostAndPort = "localhost:10009"

    val partyCHostAndPort = "localhost:10012"

    val bankHostAndPort   = "localhost:10015"

    @Bean(destroyMethod = "")
    open fun partyAProxy(): CordaRPCOps {
        val partyAClient = CordaRPCClient(NetworkHostAndPort.parse(partyAHostAndPort))
        return partyAClient.start("partya", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun partyBProxy(): CordaRPCOps {
        val partyBClient = CordaRPCClient(NetworkHostAndPort.parse(partyBHostAndPort))
        return partyBClient.start("partyb", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun partyCProxy(): CordaRPCOps {
        val partyCClient = CordaRPCClient(NetworkHostAndPort.parse(partyCHostAndPort))
        return partyCClient.start("partyc", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun bankProxy(): CordaRPCOps {
        val bankClient = CordaRPCClient(NetworkHostAndPort.parse(bankHostAndPort))
        return bankClient.start("bank", "1234").proxy
    }

}