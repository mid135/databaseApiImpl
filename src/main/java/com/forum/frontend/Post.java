package com.forum.frontend;

import com.forum.database.DBAdapter;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by mid on 26.11.14.
 */
public class Post extends HttpServlet {
    DBAdapter adapter;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        JSONObject output = new JSONObject();
        adapter = new DBAdapter();
        switch (urlRequest[4]) {
            case "details":
                response.setContentType("application/json;charset=utf-8");
                if (Integer.valueOf(request.getParameter("post")) <=0) {
                    output.put("code", 1 );
                    output.put("response", "error");
                } else {
                    LinkedHashMap body = adapter.post_details(Integer.valueOf(request.getParameter("post")), request.getParameterValues("related"));

                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                    //System.out.println(output.toString());
                }
                break;
            case "listPosts":

                break;
            case "listThreads":

                break;
            case "listUser":

                break;
            case "list": {
                try {
                    ArrayList body = adapter.post_list(request.getParameter("forum")!=null?request.getParameter("forum").toString():null,
                            request.getParameter("thread")!=null?Integer.valueOf(request.getParameter("thread").toString()):null,
                            request.getParameter("since")!=null?request.getParameter("since").toString():"1970-01-01",
                            request.getParameter("limit")!=null?Integer.valueOf(request.getParameter("limit").toString()):10000,
                            request.getParameter("order")!=null?request.getParameter("order").toString():"desc"
                    );
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }

        }
        adapter.close();
        response.getWriter().println(output.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }


    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException,RuntimeException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        response.setContentType("application/json;charset=utf-8");
        DBAdapter adapter = new DBAdapter();
        JSONObject output = new JSONObject();
        JSONObject input = adapter.parseJSON(request.getReader());
        switch  (urlRequest[4]) {
            case "create": {
                try {

                    LinkedHashMap body;
                    body = adapter.post_create(input.get("date").toString(),
                            Integer.valueOf(input.get("thread").toString()),
                            input.get("message").toString(),
                            input.get("user").toString(),
                            input.get("forum").toString(),
                            Integer.valueOf(input.get("parent") != null ? input.get("parent").toString() : "0"),
                            input.get("isApproved").equals(true),
                            input.get("isHighlighted").equals(true),
                            input.get("isEdited").equals(true),
                            input.get("isSpam").equals(true),
                            input.get("isDeleted").equals(true));
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }


                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }
            case "update": {
                try {

                    LinkedHashMap body = adapter.post_update(Integer.valueOf(input.get("post").toString()),
                            input.get("message").toString()
                    );
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }
            case "remove": {
                try {

                    LinkedHashMap body = adapter.post_remove(Integer.valueOf(input.get("post").toString()));
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }
            case "restore": {
                try {

                    LinkedHashMap body = adapter.post_restore(Integer.valueOf(input.get("post").toString()));
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }
            case "vote" :{
                try {

                    LinkedHashMap body = adapter.post_vote(Integer.valueOf(input.get("vote").toString()),Integer.valueOf(input.get("post").toString()));
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 1);
                        output.put("response", "error");
                    }
                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }

        }
        adapter.close();
        response.getWriter().println(output.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
