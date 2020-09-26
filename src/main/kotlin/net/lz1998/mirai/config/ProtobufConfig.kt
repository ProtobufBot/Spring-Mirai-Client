package net.lz1998.mirai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter

@Configuration
class ProtobufConfig {
    @Bean
    fun createProtobufConverter(): ProtobufHttpMessageConverter {
        return ProtobufHttpMessageConverter();
    }
}