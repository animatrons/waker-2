package com.waker.service;

import com.waker.model.exception.TechnicalException;

public interface ITemplatingService {

    String render(Object model, String viewName) throws TechnicalException;
}
