package com.waker.dao;

import com.mongodb.*;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.util.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.jongo.Jongo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for mongodb access managers by domain name
 */
@Slf4j
public class MongodbManagerFactory {

    final static class MongodbManager implements IMongodbManager {
        private Jongo jongo = null;
        private MongoClient client = null;
        private DB db = null;

        private MongodbManager(String domain) {
            ConfigProperties configProperties = ConfigProperties.getInstance();
            try {
                String portStr = System.getenv("mongodbPort");
                String host = System.getenv("mongodbHost");
                String userName = configProperties.getProperty("mongodb.user.name");
                String password = configProperties.getProperty("mongodb.user.pwd");
                if (Optional.ofNullable(host).isEmpty() || Optional.ofNullable(portStr).isEmpty()) {
                    throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ENV_VAR_UNDEFINED);
                }
                int port = Integer.parseInt(portStr);
                MongoCredential mongoCredential = MongoCredential.createCredential(userName, domain, password.toCharArray());
                ServerAddress serverAddress = new ServerAddress(host, port);

                client = new MongoClient(serverAddress, mongoCredential, MongoClientOptions.builder().build());
                db = client.getDB(domain);
                jongo = new Jongo(db);
            } catch (TechnicalException e) {
                log.error(e.getMessage(), e);
            }
        }

        @Override
        public Jongo getJongo() {
            return jongo;
        }
        @Override
        public MongoClient getClient() {
            return client;
        }
    }

    private static MongodbManagerFactory instance = null;
    private final Map<String, IMongodbManager> dbManagers = new HashMap<>();

    private MongodbManagerFactory() {}

    public static MongodbManagerFactory getInstance() {
        return instance != null ? instance : (instance = new MongodbManagerFactory());
    }
    public IMongodbManager getManager(String domain) {
        IMongodbManager mongodbManager;
        if (dbManagers.get(domain) == null) {
            mongodbManager = new MongodbManager(domain);
            dbManagers.put(domain, mongodbManager);
        } else {
            mongodbManager = dbManagers.get(domain);
        }
        return  mongodbManager;
    }
    public IMongodbManager getManager() {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String defaultDomain = configProperties.getProperty("mongodb.db.name");
        return getManager(defaultDomain);
    }

}
