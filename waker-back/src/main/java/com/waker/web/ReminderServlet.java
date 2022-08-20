package com.waker.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.waker.app.ReminderApp;
import com.waker.model.Reminder;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.serialization.ReminderJsonAdapter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/auth/api/reminder"}, name = "reminder")
public class ReminderServlet extends HttpServlet {

    ReminderApp app = ReminderApp.getInstance();
    GsonBuilder gsonBuilder = new GsonBuilder();

    Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Content-Type", "application/json");
        gsonBuilder.registerTypeAdapter(Reminder.class, new ReminderJsonAdapter());
        gson = gsonBuilder.create();
        JsonReader jsonReader = new JsonReader(req.getReader());
        Reminder reminder = gson.fromJson(jsonReader, Reminder.class);
        ResponseDTO<Reminder> response = app.save(reminder);
        resp.getWriter().println(gson.toJson(response));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Content-Type", "application/json");
        String id = req.getParameter("id");
        ResponseDTO<Reminder> response = app.get(id);
        resp.getWriter().println(gson.toJson(response));
    }
}
