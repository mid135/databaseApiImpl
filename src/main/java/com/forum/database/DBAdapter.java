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
import java.util.Arrays;
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
            System.err.println("Ошибка базы данных"+query+args.toString());
            return -1;

        }
    }

    //system
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

    //forun
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

    //user
    public JSONObject user_create(String username, String about,Boolean isAnomymous, String name, String email) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();
        if (email.equals("richard.nixon@example.com")) System.err.println("one");
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

    //topic aka thread
    public JSONObject topic_create(String forum,String title,Boolean isClosed,Boolean isDeleted, String user, String date,String message,String slug) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,forum);
        args.add(1,title);
        args.add(2,user);
        args.add(3,date);
        args.add(4,isClosed?1:0);
        args.add(5,isDeleted?1:0);
        args.add(6,message);
        args.add(7,slug);
        int resId = doSQL("INSERT INTO `forum_db`.`Thread` (`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`) VALUES (?,?,?,?,?,?,?,?);",args);
        if (resId > 0) {
            out.put("code",0);
            args.clear();
            args.add(0,resId);
            CachedRowSetImpl topic = doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug` FROM `forum_db`.`Thread` as t1 WHERE t1.id=?;",args);
            try {
                    while (topic.next()) {
                    resp.put("date", topic.getString(5));
                    resp.put("forum", topic.getString(2));
                    resp.put("id", topic.getString(1));
                    resp.put("isDeleted", (topic.getString(7).equals("true")));
                    resp.put("isClosed", (topic.getString(6).equals("true")));
                    resp.put("message", topic.getString(8));
                    resp.put("slug", topic.getString(9));
                    resp.put("title", topic.getString(3));
                    resp.put("user", topic.getString(4));
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
    public JSONObject topic_details(String topicId,String[] related) {
        JSONObject out = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,Integer.valueOf(topicId));
        CachedRowSetImpl res =  doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes` FROM `forum_db`.`Thread` as t1 WHERE t1.id=?;",args);
        try {
            while (res.next()) {
                out.put("code", 0);
                LinkedHashMap response = new LinkedHashMap();
                LinkedHashMap user = new LinkedHashMap();
                LinkedHashMap forum = new LinkedHashMap();

                response.put("date", res.getDate(5)+" "+res.getTime(5));

                response.put("id", res.getString(1));
                response.put("isDeleted", (res.getString(7).equals("true")));
                response.put("isClosed", (res.getString(6).equals("true")));
                response.put("message", res.getString(8));
                response.put("slug", res.getString(9));
                response.put("title", res.getString(3));
                response.put("likes", res.getInt(10));
                if (related != null && Arrays.asList(related).contains("user")) {
                    args.clear();
                    args.add(0,res.getString("user"));
                    CachedRowSetImpl userRow = doSelect("SELECT `id`,`name`,`user_name`,`about`,`email`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.email=?;",args);
                    if (userRow!= null && userRow.next()) {
                        user.put("about", userRow.getString(4).equals("")? null:userRow.getString(4));
                        user.put("email", userRow.getString(3).equals("")? null:userRow.getString(5));
                        user.put("id", userRow.getString(1));
                        user.put("name", userRow.getString(2).equals("")? null:userRow.getString(2));
                        user.put("username", userRow.getString(5).equals("")? null:userRow.getString(3));
                        user.put("isAnonymous", userRow.getString(6).equals("true"));
                        response.put("user",user);
                    }
                    //TODO доделать followers, following, subscriptions
                } else {
                    response.put("user", res.getString("user"));
                }
                if (related != null && Arrays.asList(related).contains("forum")) {
                    args.clear();
                    args.add(0,res.getString("forum"));
                    CachedRowSetImpl forumRow = doSelect("SELECT `id`,`name`,`short_name`,`user_mail` FROM `forum_db`.`Forum` as t1 WHERE t1.`short_name`=?; ",args);
                    if (forumRow!= null && forumRow.next()) {
                        forum.put("id", forumRow.getString(1));
                        forum.put("name", forumRow.getString(2));
                        forum.put("short_name", forumRow.getString(3));
                        forum.put("user", forumRow.getString(4));
                        response.put("forum",forum);
                    }
                    //TODO доделать followers, following, subscriptions
                } else {
                    response.put("forum", res.getString("forum"));
                }

                out.put("response",response);
            }
        } catch (SQLException r) {
            return null;
        }
        return out;

    }
    public JSONObject post_create(String date,Integer threadId, String message,String user,String forum,
                                  Integer parent, Boolean isApproved,Boolean isHighlighted,Boolean isEdited,
                                  Boolean isSpam,Boolean isDeleted) {
        JSONObject out = new JSONObject();
        JSONObject resp = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,isApproved);
        args.add(1,isHighlighted);
        args.add(2,isEdited);
        args.add(3,isSpam);
        args.add(4,isDeleted);
        args.add(5,date);
        args.add(6,threadId);
        args.add(7,user);
        args.add(8,forum);
        args.add(9,message);

        int resId = doSQL("INSERT INTO `forum_db`.`Post` (`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread_id`,`user_email`,`forum`,`message`) VALUES (?,?,?,?,?,?,?,?,?,?);",args);
        if (resId > 0) {
            out.put("code",0);
            args.clear();
            args.add(0,resId);
            CachedRowSetImpl post = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread_id`,`user_email`,`forum`,`message` FROM `forum_db`.`Post` as t1 WHERE t1.`id`=? ",args);
            try {
                while (post.next()) {
                    resp.put("id", post.getString(0));
                    resp.put("isApproved", post.getString(1));
                    resp.put("isHighLighted", post.getString(2));
                    resp.put("isEdited", post.getString(0));
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
}
