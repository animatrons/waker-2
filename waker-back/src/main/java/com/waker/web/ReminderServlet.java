package com.waker.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.waker.app.ReminderApp;
import com.waker.model.Reminder;
import com.waker.model.dto.ReminderDTO;
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
    // TODO: make a GsonBuilderManager (among other uses to store default configs like date format)
    GsonBuilder gsonBuilder = (new GsonBuilder()).setDateFormat("yyyy-MM-dd HH:mm:ss");

    Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        gsonBuilder
                .registerTypeAdapter(ReminderDTO.class, new ReminderJsonAdapter());
        gson = gsonBuilder.create();
        JsonReader jsonReader = new JsonReader(req.getReader());
        ReminderDTO reminderDto = gson.fromJson(jsonReader, ReminderDTO.class);
        String loggedInUsersEmail = resp.getHeader("logged-in-user");
        ResponseDTO<ReminderDTO> response = app.save(reminderDto, loggedInUsersEmail);
        resp.getWriter().println(gson.toJson(response));
        resp.setStatus(response.getStatus());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Content-Type", "application/json");
        String id = req.getParameter("id");
        String loggedInUsersEmail = resp.getHeader("logged-in-user");
        ResponseDTO<ReminderDTO> response = app.get(id, loggedInUsersEmail);
        resp.getWriter().println(gson.toJson(response));
        resp.setStatus(response.getStatus());
    }
}
