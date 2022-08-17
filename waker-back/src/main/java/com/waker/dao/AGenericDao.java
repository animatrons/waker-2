package com.waker.dao;

import com.waker.model.AModel;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class AGenericDao<T extends AModel> implements IGenericDao<T> {

    protected final Class<T> targetClass;

    protected AGenericDao(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public T find(String id) throws TechnicalException {
        T result;
        T tClassInstance;
        Jongo jongo = MongodbManagerFactory.getInstance().getManager().getJongo();
        try {
            tClassInstance = this.targetClass.getDeclaredConstructor().newInstance();
            String collectionName = tClassInstance.getCollectionName();
            MongoCollection collection = jongo.getCollection(collectionName);
            result = collection.findOne(id).as(targetClass);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
        return result;
    }

    @Override
    public List<T> find(String query, Object[] parameters, String projection, String sort, int pageSize, int start) throws TechnicalException {
        List<T> result = new ArrayList<>();
        T tClassInstance;
        Jongo jongo = MongodbManagerFactory.getInstance().getManager().getJongo();
        try {
            tClassInstance = this.targetClass.getDeclaredConstructor().newInstance();
            String collectionName = tClassInstance.getCollectionName();
            MongoCollection collection = jongo.getCollection(collectionName);

            Find find = collection.find(query, parameters);
            if (pageSize > 0 && start > 0) {
                find = find.skip(pageSize * (start - 1)).limit(pageSize);
            }
            if (projection != null && !projection.isEmpty()) {
                find = find.projection(projection);
            }
            if (sort != null && !sort.isEmpty()) {
                find = find.sort(sort);
            }
            find.as(targetClass).forEach(result::add);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
        return result;
    }

    @Override
    public String addOrUpdate(T obj) throws TechnicalException {
        T tClassInstance;
        Jongo jongo = MongodbManagerFactory.getInstance().getManager().getJongo();
        String upsertKey = obj.getKey();
        try {
            tClassInstance = this.targetClass.getDeclaredConstructor().newInstance();
            String collectionName = tClassInstance.getCollectionName();
            MongoCollection collection = jongo.getCollection(collectionName);
            var writeResult = collection.save(obj);

            if (!writeResult.wasAcknowledged()) {
                throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR, "This was unexpected, please debug here");
            }
            if (Optional.ofNullable(upsertKey).isEmpty()) {
                upsertKey = Objects.requireNonNull(writeResult.getUpsertedId()).toString();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
        return upsertKey;
    }

    @Override
    public List<String> addOrUpdate(List<T> objs) throws TechnicalException {
        List<String> results = new ArrayList<>();
        if (objs != null && !objs.isEmpty()) {
            for (T obj: objs) {
                results.add(this.addOrUpdate(obj));
            }
        }
        return results;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        T tClassInstance;
        Jongo jongo = MongodbManagerFactory.getInstance().getManager().getJongo();
        try {
            tClassInstance = this.targetClass.getDeclaredConstructor().newInstance();
            String collectionName = tClassInstance.getCollectionName();
            MongoCollection collection = jongo.getCollection(collectionName);
            collection.remove(id);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
    }

    @Override
    public void delete(T obj) throws TechnicalException {
        try {
            this.delete(obj.getKey());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
    }

    @Override
    public void delete(List<T> objs) throws TechnicalException {
        if (objs != null && !objs.isEmpty()) {
            for (T t : objs) {
                this.delete(t);
            }
        }
    }

    @Override
    public long count(String query) throws TechnicalException {
        T tClassInstance;
        long result;
        Jongo jongo = MongodbManagerFactory.getInstance().getManager().getJongo();
        try {
            tClassInstance = this.targetClass.getDeclaredConstructor().newInstance();
            String collectionName = tClassInstance.getCollectionName();
            MongoCollection collection = jongo.getCollection(collectionName);
            result = collection.count(query);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.DATABASE_ERROR);
        }
        return result;
    }
}
