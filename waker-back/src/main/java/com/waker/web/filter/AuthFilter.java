package com.waker.web.filter;

import com.google.gson.Gson;
import com.waker.app.UserApp;
import com.waker.model.dto.ResponseDTO;
import jakarta.servlet.*;
import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@WebFilter("/auth/*")
public class AuthFilter implements Filter {

    Gson gson = new Gson();
    UserApp userApp = UserApp.getInstance();
    static final List<String> AUTHORIZED_ROUTES = List.of("/auth/api/registration", "/auth/api/login", "/auth/api/test");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        response.setHeader("Content-Type", "application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", " GET, OPTIONS, HEAD, PUT, POST, DELETE");
        response.addHeader("Access-Control-Allow-Headers",
                "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization");

        String tokenHeader = request.getHeader("Authorization");
        ResponseDTO<Boolean> validationResponse = userApp.validateRequest(tokenHeader);
        boolean userLoggedIn = validationResponse.getData();

        String requestURI = request.getRequestURI();
        boolean authorizedRequest = AUTHORIZED_ROUTES.stream().anyMatch(route -> route.equals(requestURI));
        if (userLoggedIn || authorizedRequest) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            servletResponse.getWriter().println(gson.toJson(validationResponse));
            ((HttpServletResponse) servletResponse).setStatus(validationResponse.getStatus());
        }
    }
}
