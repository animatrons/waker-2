package com.waker.web;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.service.IMailServiceProvider;
import com.waker.service.impl.GmailApiService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/auth/api/test")
public class TestWithMeServlet extends HttpServlet {

    Gson gson = new Gson();
    IMailServiceProvider mailService = GmailApiService.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonReader reader = new JsonReader(req.getReader());
        MailDTO mailDTO = gson.fromJson(reader, MailDTO.class);
//        ResponseDTO<?> response = mailService.send(mailDTO, true);
        ResponseDTO<?> response = mailService.init();
        resp.setStatus(response.getStatus());
        resp.getWriter().println(gson.toJson(response));
    }
}
