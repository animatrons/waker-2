package com.waker.web;

import com.google.gson.Gson;
import com.waker.app.ReminderApp;
import com.waker.model.Reminder;
import com.waker.model.dto.ReminderDTO;
import com.waker.model.dto.ResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/auth/api/reminder/missed"}, name = "reminder missed")
public class ReminderMissedServlet extends HttpServlet {

    ReminderApp app = ReminderApp.getInstance();
    Gson gson = new Gson();
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        String loggedInUsersEmail = resp.getHeader("logged-in-user");
        ResponseDTO<ReminderDTO> response = app.takeAction(true, id, loggedInUsersEmail);
        resp.getWriter().println(gson.toJson(response));
        resp.setStatus(response.getStatus());
    }
}
