package com.waker.web.auth;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.waker.app.UserApp;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.dto.UserDTO;
import com.waker.model.dto.UserOutputDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/auth/api/login"}, name = "login")
public class LoginServlet extends HttpServlet {

    UserApp app = UserApp.getInstance();
    Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonReader jsonReader = new JsonReader(req.getReader());
        UserDTO userDto = gson.fromJson(jsonReader, UserDTO.class);
        ResponseDTO<UserOutputDTO> responseDTO = app.login(userDto);
        resp.setStatus(responseDTO.getStatus());
        resp.getWriter().println(gson.toJson(responseDTO));
    }
}
