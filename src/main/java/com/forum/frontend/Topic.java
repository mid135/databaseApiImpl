package com.forum.frontend;

import com.forum.database.DBAdapter;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by mid on 09.11.14.
 */
public class Topic extends HttpServlet {

    DBAdapter adapter;

    
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        DBAdapter adapter = DBAdapter.getDBAdapter();
        switch (urlRequest[4]) {
            case "details": {

                break;
            }
            case "listPosts": {

                break;
            }
            case "list": {

                break;
            }


        }
    }


    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException,RuntimeException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        response.setContentType("application/json;charset=utf-8");
        DBAdapter adapter = DBAdapter.getDBAdapter();
        JSONObject output = new JSONObject();
        switch (urlRequest[4]) {
            case "create":{
                try {

                    //dapter.topic_create();

                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
            }
            case "close": {

            }
            case "open": {

            }
            case "restore": {

            }
            case "subscribe": {

            }
            case "update": {

            }
            case "vote": {

            }
            case "remove": {

            }
            case "unsubscribe": {

            }
        }
        response.getWriter().println(output.toString());
        response.setStatus(HttpServletResponse.SC_OK);


    }
}