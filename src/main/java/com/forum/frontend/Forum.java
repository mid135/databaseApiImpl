package com.forum.frontend;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.json.simple.*;


/**
 * Created by mid on 29.10.14.
 */
public class Forum extends HttpServlet{

    public void doGet(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        switch (urlRequest[4]) {
            case "details":
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("успех");
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
        if  (urlRequest[4].equals("create")) {
            response.setContentType("application/json");
            JSONObject obj = new JSONObject();
            JSONObject inputJSON = new JSONObject();
            StringBuffer jb = new StringBuffer();//буфер в который мы пишем raw-data
            JSONParser parser = new JSONParser();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)//читаем из поста
                    jb.append(line);//добавляем в стринг буфер
            } catch (Exception e) { /*report an error*/ }

            try {
                inputJSON = (JSONObject)parser.parse(jb.toString());

            } catch (ParseException e) {
                // crash and burn
                throw new IOException("Error parsing JSON request string");
            }
            obj.put("code",0);
            LinkedHashMap resp = new LinkedHashMap();
            resp.put("id",1);
            resp.put("name",inputJSON.get("name"));
            resp.put("short_name",inputJSON.get("short_name"));
            resp.put("user",inputJSON.get("user"));
            obj.put("response",resp);

            response.getWriter().println(obj.toString());
            response.setStatus(HttpServletResponse.SC_OK);

         }


    }


}
