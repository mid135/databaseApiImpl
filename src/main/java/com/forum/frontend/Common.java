package com.forum.frontend;

import com.forum.database.DBAdapter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by mid on 29.10.14.
 */
public class Common extends HttpServlet {
    DBAdapter adapter;



    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        adapter = new DBAdapter();
        adapter.clear();
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        adapter.close();
        response.getWriter().print("{\"code\": 0, \"response\": \"OK\"}");

    }



}
