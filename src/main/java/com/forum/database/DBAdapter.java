package com.forum.database;

import com.mysql.fabric.jdbc.FabricMySQLDriver;
import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.rowset.CachedRowSetImpl;
import jdk.nashorn.api.scripting.JSObject;
import org.jcp.xml.dsig.internal.dom.DOMBase64Transform;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.print.DocFlavor;
import javax.sql.rowset.CachedRowSet;
import javax.swing.plaf.nimbus.State;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.Buffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by mid on 29.10.14.
 */
public class DBAdapter {
    private static final String URL = "jdbc:mysql://localhost:3306/?characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1234QWer";

    private static DBAdapter instance;

    public static synchronized DBAdapter getDBAdapter() {
       if (instance == null) {
           instance =  new DBAdapter();

       }
       return instance;
    }

    public JSONObject parseJSON(BufferedReader reader) throws IOException{
        JSONObject inputJSON = new JSONObject();
        StringBuffer jb = new StringBuffer();//буфер в который мы пишем raw-data
        JSONParser parser = new JSONParser();
        String line;
        try {
            while ((line = reader.readLine()) != null)//читаем из поста
                jb.append(line);//добавляем в стринг буфер
        } catch (Exception e) { /*report an error*/ }

        try {
            inputJSON = (JSONObject)parser.parse(jb.toString());

        } catch (ParseException e) {
            // crash and burn
            throw new IOException("Error parsing JSON request string");
        }
        return inputJSON;
    }

    private CachedRowSetImpl doSelect(String query,ArrayList args) {

        try {
            Driver driver = new FabricMySQLDriver();
            DriverManager.registerDriver(driver);

        } catch (SQLException e) {
            System.err.println("Не удалось загрузить драйвер !!!");
        }
        try(Connection conection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement statement = conection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS)) {
            CachedRowSetImpl set = new CachedRowSetImpl();

            for (int i = 1; i <= args.size(); ++i) {
                statement.setObject(i, args.get(i-1));
            }
            ResultSet res = statement.executeQuery();
            set.populate(res);
            conection.close();
            return set;
        } catch (SQLException e) {
            System.err.println("Ошибка");
            return null;
        }

    }

    private int doSQL(String query,ArrayList args) {
        try {
            Driver driver = new FabricMySQLDriver();
            DriverManager.registerDriver(driver);

        } catch (SQLException e) {
            System.err.println("Не удалось загрузить драйвер !!!");
            return -1;
        }

        try(Connection conection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement statement = conection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < args.size(); ++i) {
                statement.setObject(i+1, args.get(i));

            }
            statement.executeUpdate();
            int id = 0;
            ResultSet res = statement.getGeneratedKeys();
            while (res.next()) {
                id = res.getInt(1);
            }
            conection.close();
            return  id;
        } catch (SQLException e) {
            System.err.println("Ошибка базы данных");
            return -1;

        }
    }

    public void clear() {//очистить БД
        try(Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE TABLE `forum_db`.`Forum`;");
                statement.execute("TRUNCATE TABLE `forum_db`.`Post`;");
                statement.execute("TRUNCATE TABLE `forum_db`.`Thread`;");
                statement.execute("TRUNCATE TABLE `forum_db`.`User`;");
                statement.execute("TRUNCATE TABLE `forum_db`.`thread_followers`;");
                statement.execute("TRUNCATE TABLE `forum_db`.`user_followers`;");
                statement.execute("SET NAMES utf8;");
            } catch (SQLException e) {
                System.err.println("Ошибка");
        }
    }
    public JSONObject forum_create(String name, String shortName, String user) {
        JSONObject out = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,name);
        args.add(1,shortName);
        args.add(2,user);
        int resId;
        resId = doSQL("INSERT INTO `forum_db`.Forum (`name`,`short_name`,`user_mail`) VALUES (?,?,?);", args);
        args.clear();

        args.add(0,resId);
        CachedRowSetImpl forum = doSelect("SELECT `id`,`name`,`short_name`,`user_mail` FROM `forum_db`.`Forum` as t1 WHERE t1.`id`=?;",args);//user info
        try {
            while(forum.next()) {
                out.put("code",0);
                LinkedHashMap response = new LinkedHashMap();
                response.put("id", forum.getString("id"));
                response.put("name",forum.getString("name"));
                response.put("short_name",forum.getString("short_name"));
                response.put("user",forum.getString("user_mail"));
                out.put("response",response);
            }
        } catch (SQLException e) {

        }
        return out;
    }
    public JSONObject forum_details(String forumName, String relatedUser) {
        JSONObject out = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,forumName);
        CachedRowSetImpl res =  doSelect("SELECT `id`,`name`,`short_name`,`user_mail` FROM `forum_db`.`Forum` as t1 WHERE t1.`short_name`=?; ",args);
        try {
            while (res.next()) {
                out.put("code", 0);
                LinkedHashMap response = new LinkedHashMap();
                LinkedHashMap user = new LinkedHashMap();
                response.put("id", res.getString("id"));
                response.put("name", res.getString("name"));
                response.put("short_name", res.getString("short_name"));
                if (relatedUser != null) {
                    //user.put();
                    args.clear();
                    args.add(0,res.getString("user_mail"));
                    CachedRowSetImpl forum = doSelect("SELECT `id`,`name`,`email`,`about`,`user_name`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.`email`=?; ",args);
                    if (forum!= null && forum.next()) {
                        user.put("about", forum.getString(4).equals("")? null:forum.getString(4));
                        user.put("email", forum.getString(3).equals("")? null:forum.getString(3));
                        user.put("id", forum.getString(1));
                        user.put("name", forum.getString(2).equals("")? null:forum.getString(2));
                        user.put("username", forum.getString(5).equals("")? null:forum.getString(5));
                        user.put("isAnonymous", forum.getString(6).equals("true"));
                        response.put("user",user);
                    }
                    //TODO доделать followers, following, subscriptions
                } else {
                    response.put("user", res.getString("user_mail"));
                }
                out.put("response",response);
            }
        } catch (SQLException r) {
            return null;
        }
        return out;
    }

    public JSONObject user_create(String username, String about,Boolean isAnomymous, String name, String email) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();
        out.put("code",0);
        ArrayList args = new ArrayList();
        args.add(0,email==null?"":email);
        args.add(1,name==null?"":name);
        args.add(2,about==null?"":about);
        args.add(3,username==null?"":username);
        args.add(4,isAnomymous==false? 0: 1);
        int res = doSQL("INSERT INTO `forum_db`.`User`(`email`,`name`,`about`,`user_name`,`isAnonymous`) VALUES(?,?,?,?,?);",args);
        if (res>0) {
            out.put("code",0);
            args.clear();
            args.add(0,res);
            CachedRowSetImpl user = doSelect("SELECT `id`,`name`,`user_name`,`about`,`email`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.id=?;",args);
            try {
                while (user.next()) {
                    resp.put("about", user.getString(4));
                    resp.put("email", user.getString(5));
                    resp.put("id", user.getString(1));
                    resp.put("isAnonymous", (user.getString(6).equals("1")));
                    resp.put("name", user.getString(2));
                    resp.put("username", user.getString(3));
                    out.put("response",resp);
                }
            } catch (SQLException e) {

            }
        } else {
            out.put("code",5);
            out.put("response", "User already exists");
        }
        return out;
    }

    public JSONObject user_details(String mail) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();



        ArrayList args = new ArrayList();
        args.add(0,mail);
        CachedRowSetImpl user = doSelect("SELECT `id`,`name`,`user_name`,`about`,`email`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.email=?; ",args);

        CachedRowSetImpl followers = doSelect("SELECT `follower_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`user_mail`=?;",args);
        CachedRowSetImpl following = doSelect("SELECT `user_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`follower_mail`=?;",args);
        CachedRowSetImpl threads = doSelect("SELECT `thread_id` FROM `forum_db`.`thread_followers` as t1 WHERE t1.`user_mail`=?",args);

        try {
            if (user.next()) {
                out.put("code",0);
                if (user.getString(6).equals("false")) {
                    resp.put("about", user.getString(4).equals("") ? null : user.getString(4));
                    resp.put("email", user.getString(5));
                    resp.put("id", user.getString(1));
                    resp.put("isAnonymous", false);
                    resp.put("name", user.getString(2));
                    resp.put("username", user.getString(3).equals("") ? null : user.getString(3));
                } else {
                    resp.put("email", user.getString(5));
                    resp.put("id", user.getString(1));
                    resp.put("about", null);
                    resp.put("name", null);
                    resp.put("username", null);
                    resp.put("isAnonymous", true);

                }
                out.put("response",resp);

                JSONArray followerArray = new JSONArray();
                while (followers.next()) {
                    followerArray.add(followers.getString(1));

                }
                resp.put("followers",followerArray);

                JSONArray followingArray = new JSONArray();
                while (following.next()) {
                    followingArray.add(following.getString(1));

                }
                resp.put("following",followingArray);
                JSONArray subscriptionArray = new JSONArray();

                while (threads.next()) {
                    subscriptionArray.add(threads.getInt(1));

                }
                resp.put("subscriptions",subscriptionArray);
            } else {
                out.put("code",1);
                out.put("response","user not found");
            }
        } catch (SQLException e) {

        }

        return out;
    }

    public JSONObject topic_create(String forum,String title,Boolean isClosed, String user, String date,String message,String slug) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();



        ArrayList args = new ArrayList();


        return out;

    }
}
