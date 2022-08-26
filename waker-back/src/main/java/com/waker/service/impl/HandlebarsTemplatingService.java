package com.waker.service.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.ITemplatingService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class HandlebarsTemplatingService implements ITemplatingService {

    private static HandlebarsTemplatingService instance = null;
    private HandlebarsTemplatingService() {
        this("/");
    }
    public static HandlebarsTemplatingService getInstance() {
        if (instance == null)
            instance = new HandlebarsTemplatingService();
        return instance;
    }

    private final Handlebars handlebars;
    /**
     * Constructs a handlebars template engine
     *
     * @param resourceRoot the resource root
     */
    private HandlebarsTemplatingService(String resourceRoot) {
        TemplateLoader templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(resourceRoot);
        templateLoader.setSuffix(null);
        handlebars = new Handlebars(templateLoader);
    }

    @Override
    public String render(Object model, String viewName) throws TechnicalException {
        try {
            Template template = handlebars.compile(viewName);
            return template.apply(model);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.UNDEFINED_EXCEPTION, " Error occurred while rendering template: " + e.getMessage());
        }
    }
}
