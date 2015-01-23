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
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        JSONObject output  = new JSONObject();
        adapter = new DBAdapter();
        LinkedHashMap body;
        ArrayList body_arr;
        switch (urlRequest[4]) {
                case "details":
                    body = adapter.forum_details(request.getParameter("forum").toString(), request.getParameter("related") != null ? request.getParameterValues("related") : null);
                    response.setStatus(HttpServletResponse.SC_OK);
                    if (body!=null) {
                        output.put("code",0);
                        output.put("response", body);
                    } else {
                        output.put("code",1);
                        output.put("response","error");
                    }
                    break;
                case "listUsers":
                    body_arr = adapter.forum_listUsers(request.getParameter("forum").toString(),
                                request.getParameter("since_id") != null ? Integer.valueOf(request.getParameter("since_id").toString()) : 0,
                                request.getParameter("limit") != null ? Integer.valueOf(request.getParameter("limit").toString()) : 10000,
                                request.getParameter("order") != null ? request.getParameter("order").toString() : "desc"
                        );
                        if (body_arr != null) {
                            output.put("code", 0);
                            output.put("response", body_arr);
                        } else {
                            output.put("code", 1);
                            output.put("response", "error");
                        }

                    break;
                case "listThreads":

                        body_arr = adapter.forum_listTopics(request.getParameter("forum").toString(),
                                request.getParameter("since") != null ? request.getParameter("since").toString() : "1970-01-01",
                                request.getParameter("limit") != null ? Integer.valueOf(request.getParameter("limit").toString()) : 10000,
                                request.getParameter("order") != null ? request.getParameter("order").toString() : "desc",
                                request.getParameter("related") != null ? request.getParameterValues("related") : null
                        );
                        if (body_arr != null) {
                            output.put("code", 0);
                            output.put("response", body_arr);
                        } else {
                            output.put("code", 1);
                            output.put("response", "error");
                        }
                    break;
                case "listPosts":
                    body_arr = adapter.forum_listPosts(request.getParameter("forum").toString(),
                            request.getParameter("since") != null ? request.getParameter("since").toString() : "1970-01-01",
                            request.getParameter("limit") != null ? Integer.valueOf(request.getParameter("limit").toString()) : 10000,
                            request.getParameter("order") != null ? request.getParameter("order").toString() : "desc",
                            request.getParameter("related") != null ? request.getParameterValues("related") : null
                    );
                    if (body_arr != null) {
                        output.put("code", 0);
                        output.put("response", body_arr);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }

                    break;
            }

        adapter.close();
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(output.toString());
    }


    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException,RuntimeException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        adapter=new DBAdapter();
        JSONObject output = new JSONObject();
        if  (urlRequest[4].equals("create")) {
            response.setContentType("application/json;charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            JSONObject input=adapter.parseJSON(request.getReader());
            LinkedHashMap body =adapter.forum_create(input.get("name").toString(),input.get("short_name").toString(),input.get("user").toString());
            if (body!=null) {
                output.put("code", 0);
                output.put("response", body);
            } else {
                output.put("code",1);
                output.put("response","error");
            }
         }
        adapter.close();
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(output.toString());


    }


}
