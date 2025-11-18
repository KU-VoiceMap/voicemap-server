package org.ku.voicemap.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoiceMapDataSourceConfig {

    private static final String VOICEMAP_HIKARI_CONFIG = "voiceMapHikariConfig";

    @Bean(name = VOICEMAP_HIKARI_CONFIG)
    @ConfigurationProperties(prefix = "voicemap.datasource")
    public HikariConfig voiceMapHikariConfig(){
        return new HikariConfig();
    }

    @Bean
    public HikariDataSource voiceMapDataSource(@Qualifier(VOICEMAP_HIKARI_CONFIG) HikariConfig hikariConfig){
        return new HikariDataSource(hikariConfig);
    }
}
