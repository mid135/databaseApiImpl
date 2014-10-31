package com.forum.Main;

import com.forum.server.ServerClass;

/**
 * Created by narek on 15.10.14.
 */


public class Main {


    public static void main(String args[]) throws Exception {

        ServerClass serverObj = new ServerClass();
        serverObj.runServer();


    }
}