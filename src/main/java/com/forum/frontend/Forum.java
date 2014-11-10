package com.forum.frontend;
import com.forum.database.DBAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.json.simple.*;


/**
 * Created by mid on 29.10.14.
 */
public class Forum extends HttpServlet{
    DBAdapter adapter;



    public void doGet(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject output  = new JSONObject();
        try {
            switch (urlRequest[4]) {
                case "details":
                    //JSONObject input=adapter.parseJSON(request.getReader());


                    output = adapter.forum_details(request.getParameter("forum").toString(), request.getParameter("related") != null ? request.getParameter("related").toString() : null);
                    response.getWriter().println(output.toString());
                    response.setStatus(HttpServletResponse.SC_OK);
                    break;
                case "listPosts":

                    break;
                case "listThreads":

                    break;
                case "listUser":

                    break;

            }
            return;
        } catch (NullPointerException e) {
            //output.clear();
            output.put("code",2);
            output.put("response","invalid query");
            response.getWriter().println(output.toString());
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }


    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException,RuntimeException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        adapter=DBAdapter.getDBAdapter();
        if  (urlRequest[4].equals("create")) {
            response.setContentType("application/json");

            JSONObject input=adapter.parseJSON(request.getReader());
            JSONObject output;
            output = adapter.forum_create(input.get("name").toString(),input.get("short_name").toString(),input.get("user").toString());
            response.getWriter().println(output.toString());
            response.setStatus(HttpServletResponse.SC_OK);

         }


    }


}
