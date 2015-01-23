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
 * Created by mid on 06.11.14.
 */


public class User  extends HttpServlet {

    DBAdapter adapter;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        JSONObject output = new JSONObject();
        DBAdapter adapter = new DBAdapter();
        response.setContentType("application/json;charset=utf-8");
        switch (urlRequest[4]) {
            case "details": {
                ArrayList args = new ArrayList();
                args.add(request.getParameter("user"));
                //System.out.println("user");
                LinkedHashMap body = adapter.user_details(args);

                if (body != null) {
                    output.put("code", 0);
                    output.put("response", body);
                } else {
                    output.put("code", 1);
                    output.put("response", "error");
                }
                //System.out.println(output.toString());
                break;
            }
            case "listPosts": {
                ArrayList args = new ArrayList();

                ArrayList body = adapter.user_listPosts(request.getParameter("user"),
                        request.getParameter("limit")!=null?Integer.valueOf(request.getParameter("limit")):10000,
                        request.getParameter("order")!=null?(request.getParameter("order").equals("asc")?"asc":"desc"):"desc",
                        request.getParameter("since")!=null?request.getParameter("since"):"1970-01-01 00:00:00",
                        request.getParameterValues("related") != null ? request.getParameterValues("related") : null
                );

                if (body != null) {
                    output.put("code", 0);
                    output.put("response", body);
                } else {
                    output.put("code", 1);
                    output.put("response", "error");
                }
                break;
            }


            case "listFollowers": {
                ArrayList args = new ArrayList();

                ArrayList body = adapter.user_listFollowers(request.getParameter("user"),
                        request.getParameter("limit")!=null?Integer.valueOf(request.getParameter("limit")):null,
                        request.getParameter("order")!=null?request.getParameter("order"):"desc",
                        request.getParameter("since_id")!=null?Integer.valueOf(request.getParameter("since_id")):null
                );

                if (body != null) {
                    output.put("code", 0);
                    output.put("response", body);
                } else {
                    output.put("code", 1);
                    output.put("response", "error");
                }
                break;
            }
            case "listFollowing": {
                ArrayList args = new ArrayList();

                ArrayList body = adapter.user_listFollowing(request.getParameter("user"),
                        request.getParameter("limit")!=null?Integer.valueOf(request.getParameter("limit")):10000,
                        request.getParameter("order")!=null?request.getParameter("order"):"desc",
                        request.getParameter("since_id")!=null?Integer.valueOf(request.getParameter("since_id")):null
                );

                if (body != null) {
                    output.put("code", 0);
                    output.put("response", body);
                } else {
                    output.put("code", 1);
                    output.put("response", "error");
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
        switch  (urlRequest[4]) {
            case "create": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body;
                    Boolean isAnonymous = input.get("isAnonymous") != null ? (input.get("isAnonymous").toString() == "true" ? true : false) : false;
                    if (!isAnonymous) {
                        body = adapter.user_create(input.get("username").toString(), input.get("about").toString(), isAnonymous, input.get("name").toString(), input.get("email").toString());
                    } else {
                        body = adapter.user_create(null, null, isAnonymous, null, input.get("email").toString());
                    }

                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 5);
                        output.put("response", "error");
                    }


                } catch (NullPointerException e) {
                    output.clear();
                    output.put("code", 2);
                    output.put("response", "invalid query");
                }
                break;
            }
            case "updateProfile":{
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.user_update(input.get("about").toString(),
                            input.get("user").toString(),
                            input.get("name").toString()
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
            case "follow": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.user_follow(input.get("follower").toString(),
                            input.get("followee").toString()
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
            case "unfollow": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.user_unfollow(input.get("follower").toString(),
                            input.get("followee").toString()
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
}




