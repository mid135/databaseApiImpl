package com.forum.frontend;

import com.forum.database.DBAdapter;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by mid on 06.11.14.
 */


public class User  extends HttpServlet {

    DBAdapter adapter;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        DBAdapter adapter = DBAdapter.getDBAdapter();
        switch (urlRequest[4]) {
            case "details":
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                JSONObject out = new JSONObject();
                String us = request.getParameter("user");
                out = adapter.user_details(us);
                response.getWriter().println(out.toString());
                break;
            case "listPosts":

                break;
            case "listThreads":

                break;
            case "listUser":

                break;

        }
    }


    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException,RuntimeException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        response.setContentType("application/json;charset=utf-8");
        DBAdapter adapter = DBAdapter.getDBAdapter();
        JSONObject output = new JSONObject();
        if  (urlRequest[4].equals("create")) {

            try {
                JSONObject input = adapter.parseJSON(request.getReader());

                Boolean isAnonymous = input.get("isAnonymous") != null ? (input.get("isAnonymous").toString() == "true" ? true : false) : false;
                if (!isAnonymous) {
                    output = adapter.user_create(input.get("username").toString(), input.get("about").toString(), isAnonymous, input.get("name").toString(), input.get("email").toString());
                } else {
                    output = adapter.user_create(null, null, isAnonymous, "", input.get("email").toString());
                }
            } catch (NullPointerException e) {
                output.clear();
                output.put("code", 2);
                output.put("response", "invalid query");
            }
            response.getWriter().println(output.toString());
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
}




