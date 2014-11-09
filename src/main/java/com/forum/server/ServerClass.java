package com.forum.server;

import com.forum.database.DBAdapter;
import com.forum.frontend.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Created by narek on 15.10.14.
 */
public class ServerClass {
    public void runServer() throws Exception {
        Frontend frontend = new Frontend();


        DBAdapter adapter = new DBAdapter();

        Common common = new Common(adapter);
        Forum forum = new Forum(adapter);
        User user = new User(adapter);

        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        server.setHandler(context);
        //context.addServlet(new ServletHolder(frontend), "/*");
        context.addServlet(new ServletHolder(common),"/db/api/clear/");
        context.addServlet(new ServletHolder(forum),"/db/api/forum/*");
        context.addServlet(new ServletHolder(user),"/db/api/user/*");

        server.start();
        server.join();
    }
}
