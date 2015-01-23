package com.forum.frontend;

import com.forum.database.DBAdapter;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by mid on 09.11.14.
 */
public class Topic extends HttpServlet {

    DBAdapter adapter;

    
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        String[] urlRequest = request.getRequestURI().toString().split("/");
        DBAdapter adapter = new DBAdapter();
        JSONObject output = new JSONObject();
        switch (urlRequest[4]) {
            case "details": {
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                //List param = Arrays.asList(request.getParameterValues("related"));

                Integer id = Integer.valueOf(request.getParameter("thread"));
                //TODO нормальная проверка related - с помощью отдельной функции, в которую буем передавать то, что может быть в related
                if (request.getParameterValues("related") != null) {
                    if (Arrays.asList(request.getParameterValues("related")).contains("thread")) {
                        output.put("code", 3);
                        output.put("response", "error");
                        break;
                    }
                }

                if (id <= 0) {
                    output.put("code", 1);
                    output.put("response", "error");
                } else {
                    LinkedHashMap body = adapter.topic_details(id, request.getParameterValues("related") != null ? request.getParameterValues("related") : null);
                    if (body != null) {
                        output.put("code", 0);
                        output.put("response", body);
                    } else {
                        output.put("code", 3);
                        output.put("response", "error");
                    }
                }

                break;
            }
            case "listPosts": {
                try {
                    ArrayList body = adapter.topic_listPosts(
                            Integer.valueOf(request.getParameter("thread").toString()),
                            request.getParameter("since")!=null?request.getParameter("since").toString():"1970-01-01",
                            request.getParameter("limit")!=null?Integer.valueOf(request.getParameter("limit").toString()):10000,
                            request.getParameter("order")!=null?request.getParameter("order").toString():"desc",
                            request.getParameter("sort")!=null?request.getParameter("sort").toString():"flat"
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
            case "list": {
                try {
                    ArrayList body = adapter.topic_list(request.getParameter("forum")!=null?request.getParameter("forum").toString():null,
                            request.getParameter("user")!=null?request.getParameter("user").toString():null,
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
        switch (urlRequest[4]) {
            case "create":{
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    //TODO читаемый вызов функции
                    LinkedHashMap body=adapter.topic_create(input.get("forum").toString(),
                            input.get("title").toString(),
                            Boolean.parseBoolean(input.get("isClosed").toString()),
                            Boolean.parseBoolean(input.get("isDeleted").toString()),
                            input.get("user").toString(),
                            input.get("date").toString(),
                            input.get("message").toString(),
                            input.get("slug").toString()
                    );
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
                break;
            }
            case "close": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_close(Integer.valueOf(input.get("thread").toString()));
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
            case "open": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_open(Integer.valueOf(input.get("thread").toString()));
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
            case "remove":{
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_remove(Integer.valueOf(input.get("thread").toString()));
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
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_restore(Integer.valueOf(input.get("thread").toString()));
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
            case "subscribe": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_subscribe(input.get("user").toString(),Integer.valueOf(input.get("thread").toString()));
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
            case "unsubscribe": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_unsubscribe(input.get("user").toString(),Integer.valueOf(input.get("thread").toString()));
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
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_update(Integer.valueOf(input.get("thread").toString()),
                            input.get("message").toString(),
                            input.get("slug").toString()
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
            case "vote": {
                try {
                    JSONObject input = adapter.parseJSON(request.getReader());
                    LinkedHashMap body = adapter.topic_vote(Integer.valueOf(input.get("vote").toString()),Integer.valueOf(input.get("thread").toString()));
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
            }



        }
        adapter.close();
        response.getWriter().println(output.toString());
        response.setStatus(HttpServletResponse.SC_OK);


    }
}