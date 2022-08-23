package com.waker.web.auth;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.waker.app.UserApp;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.dto.UserDTO;
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
        resp.addHeader("Content-Type", "application/json");
        JsonReader jsonReader = new JsonReader(req.getReader());
        UserDTO user = gson.fromJson(jsonReader, UserDTO.class);
        ResponseDTO<UserDTO> response = this.app.register(user);
        resp.getWriter().println(gson.toJson(response));
    }
}
