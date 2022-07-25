package com.waker.dao;

import com.mongodb.MongoClient;
import org.jongo.Jongo;

/**
 *
 */
public interface IMongodbManager {

    /**
     * Get the jongo mongodb driver
     */
    Jongo getJongo();
    MongoClient getClient();
}
