package com.waker.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class ConfigProperties {
    private final Properties properties = new Properties();
    private static ConfigProperties instance = null;

    private ConfigProperties () {
        InputStream inputStream = null;
        try {
            inputStream = ConfigProperties.class.getClassLoader().getResourceAsStream("config.properties");
            properties.load(inputStream);
        } catch (IOException | NullPointerException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                Objects.requireNonNull(inputStream).close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static ConfigProperties getInstance() {
        return instance != null ? instance : (instance = new ConfigProperties());
    }
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

}
