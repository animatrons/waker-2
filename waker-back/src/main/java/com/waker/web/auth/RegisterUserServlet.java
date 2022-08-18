package com.waker.web.auth;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.waker.app.UserApp;
import com.waker.model.User;
import com.waker.model.dto.ResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/auth/api/registration", name = "userRegistration")
public class RegisterUserServlet extends HttpServlet {

    UserApp app = UserApp.getInstance();
    Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonReader jsonReader = new JsonReader(req.getReader());
        User user = gson.fromJson(jsonReader, User.class);
        ResponseDTO<User> response = this.app.register(user);
        resp.getWriter().println(response);
    }
}
