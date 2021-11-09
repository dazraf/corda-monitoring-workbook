package com.template.webserver

import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.corda.client.jackson.JacksonSupport
import net.corda.client.jackson.internal.CordaModule
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
private open class Starter 

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Starter::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)
}
