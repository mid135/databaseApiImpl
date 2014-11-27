package com.forum.frontend;

import com.forum.database.DBAdapter;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Created by mid on 06.11.14.
 */


public class User  extends HttpServlet {

    DBAdapter adapter;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        JSONObject output = new JSONObject();
        DBAdapter adapter = DBAdapter.getDBAdapter();
        switch (urlRequest[4]) {
            case "details":
                response.setContentType("application/json;charset=utf-8");


                LinkedHashMap body = adapter.user_details(request.getParameter("user"));

                if (body!=null) {
                    output.put("code",0);
                    output.put("response", body);
                } else {
                    output.put("code",1);
                    output.put("response","error");
                }
                //System.out.println(output.toString());
                break;
            case "listPosts":

                break;
            case "listThreads":

                break;
            case "listUser":

                break;

        }
        response.getWriter().println(output.toString());
        response.setStatus(HttpServletResponse.SC_OK);
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
                LinkedHashMap body;
                Boolean isAnonymous = input.get("isAnonymous") != null ? (input.get("isAnonymous").toString() == "true" ? true : false) : false;
                if (!isAnonymous) {
                    body = adapter.user_create(input.get("username").toString(), input.get("about").toString(), isAnonymous, input.get("name").toString(), input.get("email").toString());
                } else {
                     body = adapter.user_create(null, null, isAnonymous, null, input.get("email").toString());
                }

                if (body!=null) {
                    output.put("code",0);
                    output.put("response", body);
                } else {
                    output.put("code",1);
                    output.put("response","error");
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




