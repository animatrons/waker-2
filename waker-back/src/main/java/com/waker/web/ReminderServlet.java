package com.waker.web;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.waker.app.ReminderApp;
import com.waker.model.Reminder;
import com.waker.model.dto.ResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/auth/api/reminder"}, name = "reminder")
public class ReminderServlet extends HttpServlet {

    ReminderApp app = ReminderApp.getInstance();
    Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonReader jsonReader = new JsonReader(req.getReader());
        Reminder reminder = gson.fromJson(jsonReader, Reminder.class);
        ResponseDTO<Reminder> response = app.save(reminder);
        resp.getWriter().println(response);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        ResponseDTO<Reminder> response = app.get(id);
        resp.getWriter().println(response);
    }
}
