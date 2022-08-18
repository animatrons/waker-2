package com.waker.service.impl;

import com.waker.dao.IGenericDao;
import com.waker.model.AModel;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IBaseService;

import java.util.List;

public abstract class BaseService<T extends AModel, U extends IGenericDao<T>> implements IBaseService<T> {

    protected U dao = null;

    @Override
    public String addOrUpdate(T obj) throws TechnicalException, BusinessException {
        return dao.addOrUpdate(obj);
    }

    @Override
    public List<String> addOrUpdate(List<T> objects) throws TechnicalException, BusinessException {
        return dao.addOrUpdate(objects);
    }

    @Override
    public T get(String id) throws TechnicalException, BusinessException {
        return dao.find(id);
    }

    @Override
    public List<T> search(String query, Object[] parameters, String projection, String sort, int pageSize, int start) throws TechnicalException, BusinessException {
        return dao.find(query, parameters, projection, sort, pageSize, start);
    }

    @Override
    public void delete(String id) throws TechnicalException, BusinessException {
        dao.delete(id);
    }

    @Override
    public void delete(List<T> objects) throws TechnicalException, BusinessException {
        dao.delete(objects);
    }

    @Override
    public long count(String query) throws TechnicalException, BusinessException {
        return dao.count(query);
    }
}
