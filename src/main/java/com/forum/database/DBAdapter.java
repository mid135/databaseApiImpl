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
import java.util.*;

/**
 * Created by mid on 29.10.14.
 */
public class DBAdapter {
    private static final String URL = "jdbc:mysql://localhost:3306/?characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1234QWer";

    private static DBAdapter instance;
    Connection conection;
    public  DBAdapter() {
        try {
            Driver driver = new FabricMySQLDriver();
            DriverManager.registerDriver(driver);
            this.conection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Не удалось загрузить драйвер !!!");
        }
        ArrayList ef = new ArrayList();
       // doSQL("USE `forum_db`",ef);

    }
    public void close() {
        try {
            this.conection.close();
        } catch (SQLException e) {};
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

            inputJSON = (JSONObject)parser.parse(jb.toString().replace("\'","\""));

        } catch (ParseException e) {
            System.err.println(jb.toString());
            throw new IOException("Error parsing JSON request string");
        }

        return inputJSON;
    }

    private CachedRowSetImpl doSelect(String query,ArrayList args) {


        try(
            PreparedStatement statement = conection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS)) {
            CachedRowSetImpl set = new CachedRowSetImpl();

            for (int i = 1; i <= args.size(); ++i) {
                statement.setObject(i, args.get(i-1));
            }
            ResultSet res = statement.executeQuery();
            set.populate(res);
            statement.close();
            return set;
        } catch (SQLException e) {
            System.err.println("Ошибка"+query+args);
            return null;
        }

    }

    private synchronized int doSQL(String query,ArrayList args) {

        try(
            PreparedStatement statement = conection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < args.size(); ++i) {
                statement.setObject(i+1, args.get(i));

            }
                statement.executeUpdate();
            int id = 0;
            ResultSet res = statement.getGeneratedKeys();

            if (res.next()) {//предполагается, что update на одну строку
                id = res.getInt(1);
            }
            statement.close();
            return  id;
        } catch (SQLException e) {
            //System.err.println("Ошибка базы данных"+query+args.toString());

            return -1;
        } catch (NullPointerException n) {
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
    public LinkedHashMap forum_create(String name, String shortName, String user) {
        JSONObject out = new JSONObject();
        ArrayList args = new ArrayList();
        args.add(0,name);
        args.add(1,shortName);
        args.add(2,user);
        int resId;
        resId = doSQL("INSERT INTO `forum_db`.Forum (`name`,`short_name`,`user_mail`) VALUES (?,?,?);", args);
        LinkedHashMap response = new LinkedHashMap();
        args.add(0,resId);
        if(resId>0) {
                response.put("id", resId);
                response.put("name",args.get(0));
                response.put("short_name",args.get(1));
                response.put("user",args.get(2));

        } else {
            return null;
        }

        return response;
    }
    public LinkedHashMap forum_details(String forumName, String[] relatedUser) {
        ArrayList args = new ArrayList();
        args.add(0,forumName);
        LinkedHashMap response = new LinkedHashMap();
        CachedRowSetImpl res =  doSelect("SELECT `id`,`name`,`short_name`,`user_mail` FROM `forum_db`.`Forum` as t1 WHERE t1.`short_name`=?; ",args);
        try {
            while (res.next()) {
                response.put("id", res.getInt("id"));
                response.put("name", res.getString("name"));
                response.put("short_name", res.getString("short_name"));

                if (relatedUser!=null && Arrays.asList(relatedUser).contains("user")) {
                    args.clear();
                    args.add(res.getString("user_mail"));

                    response.put("user",this.user_details(args));
                } else {
                    response.put("user", res.getString("user_mail"));
                }
            }
        } catch (SQLException r) {
            return null;
        }
        return response;
    }

    public ArrayList forum_listUsers(String forum, Integer since_id, Integer limit,String order) {
        ArrayList result = new ArrayList();

        ArrayList args = new ArrayList();
        args.add(forum);
        args.add(since_id);
        args.add(limit);
        CachedRowSetImpl users;
        try {
            if (order.equals("asc")) {
                //TODO WTF    -1 IN ID???????
                users=doSelect("SELECT t2.`email`,t2.`name` FROM `forum_db`.`Post` as t1 LEFT JOIN `forum_db`.`User` as t2 ON t1.user_email=t2.email WHERE t1.forum=? and t2.`id`>=? GROUP BY t2.id ORDER BY t2.name ASC LIMIT ?", args);
            } else {
                users = doSelect("SELECT t2.`email`,t2.`name` FROM `forum_db`.`Post` as t1 LEFT JOIN `forum_db`.`User` as t2 ON t1.user_email=t2.email WHERE t1.forum=? and t2.`id`>=? GROUP BY t2.id ORDER BY t2.name DESC LIMIT ?", args);
            }
            args.clear();
            while (users.next()) {
                args.add(users.getString(1));
                LinkedHashMap resp = new LinkedHashMap();
                CachedRowSetImpl user = doSelect("SELECT `id`,`name`,`user_name`,`about`,`email`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.email IN (?); ", args);
                CachedRowSetImpl followers = doSelect("SELECT `follower_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`user_mail`=?;", args);
                CachedRowSetImpl following = doSelect("SELECT `user_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`follower_mail`=?;", args);
                CachedRowSetImpl threads = doSelect("SELECT `thread_id` FROM `forum_db`.`thread_followers` as t1 WHERE t1.`user_mail`=?;", args);
                while (user.next()) {
                    if (user.getString(6).equals("false")) {
                        resp.put("about", user.getString(4).equals("") ? null : user.getString(4));
                        resp.put("email", user.getString(5));
                        resp.put("id", user.getInt(1));
                        resp.put("isAnonymous", false);
                        resp.put("name", user.getString(2));
                        resp.put("username", user.getString(3).equals("") ? null : user.getString(3));
                    } else {
                        resp.put("email", user.getString(5));
                        resp.put("id", user.getInt(1));
                        resp.put("about", null);
                        resp.put("name", null);
                        resp.put("username", null);
                        resp.put("isAnonymous", true);
                    }
                    //JSONArray followerArray=new JSONArray();
                    ArrayList followerArray = new ArrayList();
                    while (followers.next()) {
                        followerArray.add(followers.getString(1));
                    }
                    resp.put("followers", followerArray);
                    ArrayList followingArray = new ArrayList();
                    //JSONArray followingArray = new JSONArray();
                    while (following.next()) {
                        followingArray.add(following.getString(1));
                    }
                    resp.put("following", followingArray);
                    HashSet subscriptionArray = new HashSet();
                    while (threads.next()) {
                        subscriptionArray.add(threads.getInt(1));
                    }
                    resp.put("subscriptions", subscriptionArray);
                    result.add(resp);
                    args.clear();
                }
            }
        } catch(SQLException e) {
            return null;
        }
        return result;
    }

    public ArrayList forum_listTopics(String forum_name,String since, Integer limit,String order,String[] related) {
        ArrayList result = new ArrayList();

        ArrayList args = new ArrayList();
        args.add(forum_name);
        args.add(since);
        args.add(limit);
        CachedRowSetImpl threads;

        try {
            if (order.equals("asc")) {
                threads=doSelect("SELECT t2.id FROM `forum_db`.`Forum` as t1 LEFT JOIN `forum_db`.`Thread` as t2 ON t2.forum=t1.`short_name` WHERE t1.`short_name`=? AND t2.`date`>? ORDER BY t2.`date` ASC LIMIT ?", args);
            } else {
                threads = doSelect("SELECT t2.id FROM `forum_db`.`Forum` as t1 LEFT JOIN `forum_db`.`Thread` as t2 ON t2.forum=t1.`short_name` WHERE t1.`short_name`=? AND t2.`date`>? ORDER BY t2.`date` DESC LIMIT ?", args);
            }
            args.clear();
            while (threads.next()) {
                result.add(topic_details(threads.getInt(1),related));
            }

        } catch(SQLException e) {
            return null;
        }
        return result;

    }

    public ArrayList forum_listPosts(String forum_name,String since, Integer limit,String order,String[] related) {
        ArrayList args = new ArrayList();
        args.add(forum_name);
        args.add(since);
        args.add(limit);
        CachedRowSetImpl res;
        if (order.equals("asc")) {
            res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.forum=? and t1.`creation_date`>?  ORDER BY t1.`creation_date` ASC LIMIT ?;", args);
        } else {
            res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.forum=? and t1.`creation_date`>?  ORDER BY t1.`creation_date` DESC LIMIT ?;", args);
        }
        args.clear();
        ArrayList result = new ArrayList();

        try {
            while (res.next()) {
                LinkedHashMap elem = new LinkedHashMap();
                elem.put("date", res.getDate(7) + " " + res.getTime(7));
                elem.put("id", res.getInt(1));
                elem.put("isApproved", res.getString(2).equals("true"));
                elem.put("isHighlighted", res.getString(3).equals("true"));
                elem.put("isEdited", res.getString(4).equals("true"));
                elem.put("isSpam", res.getString(5).equals("true"));
                elem.put("isDeleted", res.getString(6).equals("true"));
                elem.put("message", res.getString(11));
                elem.put("parent", res.getInt(12) == 0 ? null : res.getInt(12));
                elem.put("points", res.getInt(13));
                elem.put("likes", res.getInt(14));
                elem.put("dislikes", res.getInt(15));
                if (related != null && Arrays.asList(related).contains("thread")) {
                    elem.put("thread", this.topic_details(res.getInt(8), null));
                } else {
                    elem.put("thread", res.getInt(8));
                }
                if (related != null && Arrays.asList(related).contains("user")) {
                    args.clear();
                    args.add(res.getString(9));
                    elem.put("user", this.user_details(args));
                } else {
                    elem.put("user", res.getString(9));
                }
                if (related != null && Arrays.asList(related).contains("forum")) {
                    elem.put("forum", this.forum_details(res.getString(10), null));
                } else {
                    elem.put("forum", res.getString(10));
                }
                ;
                result.add(elem);

            }
        } catch (SQLException r) {
            System.out.println("error");
            return null;
        }
        return result;
    }

    public LinkedHashMap user_create(String username, String about,Boolean isAnomymous, String name, String email) {
        LinkedHashMap resp = new LinkedHashMap();
        ArrayList args = new ArrayList();
        args.add(0,email==null?"":email);
        args.add(1,name==null?"":name);
        args.add(2,about==null?"":about);
        args.add(3,username==null?"":username);
        args.add(4,isAnomymous==false? 0: 1);
        Integer res = doSQL("INSERT INTO `forum_db`.`User`(`email`,`name`,`about`,`user_name`,`isAnonymous`) VALUES(?,?,?,?,?);",args);
        if (res>0) {
            args.add(0,res);
            resp.put("about", about);
            resp.put("email", email);
            resp.put("id", res);
            resp.put("isAnonymous", isAnomymous);
            resp.put("name", name);
            resp.put("username", username);
        } else {
            return null;
        }
        return resp;
    }
    public LinkedHashMap user_details(ArrayList args) {
        LinkedHashMap resp = new LinkedHashMap();
        for (int i=0; i<args.size();++i) {
            CachedRowSetImpl user = doSelect("SELECT `id`,`name`,`user_name`,`about`,`email`,`isAnonymous` FROM `forum_db`.`User` as t1 WHERE t1.email IN (?); ", args);
            CachedRowSetImpl followers = doSelect("SELECT `follower_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`user_mail`=?;", args);
            CachedRowSetImpl following = doSelect("SELECT `user_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.`follower_mail`=?;", args);
            CachedRowSetImpl threads = doSelect("SELECT `thread_id` FROM `forum_db`.`thread_followers` as t1 WHERE t1.`user_mail`=?;", args);
            try {
                if (user.next()) {
                    if (user.getString(6).equals("false")) {
                        resp.put("about", user.getString(4).equals("") ? null : user.getString(4));
                        resp.put("email", user.getString(5));
                        resp.put("id", user.getInt(1));
                        resp.put("isAnonymous", false);
                        resp.put("name", user.getString(2));
                        resp.put("username", user.getString(3).equals("") ? null : user.getString(3));
                    } else {
                        resp.put("email", user.getString(5));
                        resp.put("id", user.getInt(1));
                        resp.put("about", null);
                        resp.put("name", null);
                        resp.put("username", null);
                        resp.put("isAnonymous", true);
                    }
                    //JSONArray followerArray=new JSONArray();
                    ArrayList followerArray = new ArrayList();
                    while (followers.next()) {
                        followerArray.add(followers.getString(1));
                    }
                    resp.put("followers", followerArray);
                    ArrayList followingArray = new ArrayList();
                    //JSONArray followingArray = new JSONArray();
                    while (following.next()) {
                        followingArray.add(following.getString(1));
                    }
                    resp.put("following", followingArray);
                    HashSet subscriptionArray = new HashSet();
                    while (threads.next()) {
                        subscriptionArray.add(threads.getInt(1));
                    }
                    resp.put("subscriptions", subscriptionArray);
                } else {
                    return null;
                }
            } catch (SQLException e) {
                System.out.println("error");
            }
        }
        return resp;
    }

    public LinkedHashMap user_update(String about, String email, String name) {
        ArrayList args = new ArrayList();

        args.add(0,name);
        args.add(1,about);
        args.add(2,email);

        Integer id = doSQL("UPDATE `forum_db`.`User` as t1 SET t1.`name`=?,t1.`about`=? WHERE t1.`email`=?",args);
        args.clear();
        args.add(email);
        return user_details(args);

    }

    public LinkedHashMap user_follow(String follower,String followee) {
        ArrayList args = new ArrayList();

        args.add(0,followee);
        args.add(1,follower);

        Integer id = doSQL("INSERT INTO `forum_db`.`user_followers` (`user_mail`,`follower_mail`) VALUES (?,?);",args);
        args.clear();
        args.add(followee);
        return user_details(args);
    }
    public LinkedHashMap user_unfollow(String follower,String followee) {
        ArrayList args = new ArrayList();

        args.add(0,followee);
        args.add(1,follower);

        Integer id = doSQL("DELETE FROM `forum_db`.`user_followers` WHERE `user_mail`=? and `follower_mail`=?;",args);
        args.clear();
        args.add(followee);
        return user_details(args);
    }

    //TODO доделать поддержку аргументов
    public ArrayList user_listFollowers(String email, Integer limit,String order, Integer since_id) {
        ArrayList args = new ArrayList();
        args.add(email);
        CachedRowSetImpl subc = doSelect("SELECT t1.`follower_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.user_mail=?;",args);
        args.clear();
        try {
            while (subc.next()) {
                args.add(subc.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("error");
        }
        ArrayList resp = new ArrayList();
        for (int i=0; i < args.size();++i) {
            ArrayList param = new ArrayList();
            param.add(args.get(i));
            resp.add(user_details(param));
            param.clear();
        }
        return resp;
    }
    public ArrayList user_listFollowing(String email, Integer limit,String order, Integer since_id) {
        ArrayList args = new ArrayList();
        args.add(email);
        CachedRowSetImpl subc = doSelect("SELECT t1.`user_mail` FROM `forum_db`.`user_followers` as t1 WHERE t1.follower_mail=?;",args);
        args.clear();
        try {
            while (subc.next()) {
                args.add(subc.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("error");
        }
        ArrayList resp = new ArrayList();
        for (int i=0; i < args.size();++i) {
            ArrayList param = new ArrayList();
            param.add(args.get(i));
            resp.add(user_details(param));
            param.clear();
        }
        return resp;
    }
    public ArrayList user_listPosts(String email, Integer limit,String order, String since,String[] related) {
        ArrayList args = new ArrayList();
        args.add(email);
        args.add(since);
        args.add(limit);
        CachedRowSetImpl res;
        if (order.equals("asc")) {
            res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.user_email=? and t1.`creation_date`>?  ORDER BY t1.`creation_date` ASC LIMIT ?;", args);
        } else {
            res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.user_email=? and t1.`creation_date`>?  ORDER BY t1.`creation_date` DESC LIMIT ?;",args);
        }
        args.clear();
        ArrayList result = new ArrayList();

        try {
            while (res.next()) {
                LinkedHashMap elem = new LinkedHashMap();
                elem.put("date", res.getDate(7)+" "+res.getTime(7));
                elem.put("id", res.getInt(1));
                elem.put("isApproved", res.getString(2).equals("true"));
                elem.put("isHighlighted", res.getString(3).equals("true"));
                elem.put("isEdited", res.getString(4).equals("true"));
                elem.put("isSpam", res.getString(5).equals("true"));
                elem.put("isDeleted", res.getString(6).equals("true"));
                elem.put("message", res.getString(11));
                elem.put("parent",res.getInt(12)==0?null:res.getInt(12));
                elem.put("points",res.getInt(13));
                elem.put("likes",res.getInt(14));
                elem.put("dislikes",res.getInt(15));
                if (related != null && Arrays.asList(related).contains("thread")) {
                    elem.put("thread",this.topic_details(res.getInt(8),null));
                } else {
                    elem.put("thread", res.getInt(8));
                }
                if (related != null && Arrays.asList(related).contains("user")) {
                    args.clear();
                    args.add(res.getString(9));
                    elem.put("user",this.user_details(args));
                } else {
                    elem.put("user", res.getString(9));
                }
                if (related != null && Arrays.asList(related).contains("forum")) {
                    elem.put("forum",this.forum_details(res.getString(10),null));
                } else {
                    elem.put("forum", res.getString(10));
                };
                result.add(elem);

            }
        } catch (SQLException r) {
            System.out.println("error");
            return null;
        }
        return result;
    }

    //topic aka thread
    public LinkedHashMap topic_create(String forum,String title,Boolean isClosed,Boolean isDeleted, String user, String date,String message,String slug) {

        LinkedHashMap resp = new LinkedHashMap();
        ArrayList args = new ArrayList();
        args.add(0,forum);
        args.add(1,title);
        args.add(2,user);
        args.add(3,date);
        args.add(4,isClosed?1:0);
        args.add(5,isDeleted?1:0);
        args.add(6,message);
        args.add(7,slug);
        int resId = doSQL("INSERT INTO `forum_db`.`Thread` (`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`posts`) VALUES (?,?,?,?,?,?,?,?,0);",args);
        if (resId > 0) {
                    resp.put("date", args.get(3));
                    resp.put("forum", args.get(0));
                    resp.put("id", resId);
                    resp.put("isDeleted", (args.get(5).equals("true")));
                    resp.put("isClosed", (args.get(4).equals("true")));
                    resp.put("message", args.get(6));
                    resp.put("slug", args.get(7));
                    resp.put("title", args.get(1));
                    resp.put("user", args.get(2));

        } else {
            return null;
        }
        return resp;
    }
    public LinkedHashMap    topic_details(Integer topicId,String[] related) {
        LinkedHashMap response = new LinkedHashMap();
        ArrayList args = new ArrayList();
        args.add(0,Integer.valueOf(topicId));
        CachedRowSetImpl res =  doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes`,`likes`-`dislikes`,`dislikes` as points,`posts` FROM `forum_db`.`Thread` as t1 WHERE t1.id=?;",args);
        try {
            while (res.next()) {
                LinkedHashMap user = new LinkedHashMap();
                LinkedHashMap forum = new LinkedHashMap();
                response.put("date", res.getDate(5)+" "+res.getTime(5));
                response.put("id", res.getInt(1));
                //response.put("posts",res.getString(7).equals("true")==true?0:res.getInt(13));
                response.put("posts",res.getInt(13));//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                response.put("isDeleted", (res.getString(7).equals("true")));
                response.put("isClosed", (res.getString(6).equals("true")));
                response.put("message", res.getString(8));
                response.put("slug", res.getString(9));
                response.put("title", res.getString(3));
                response.put("likes", res.getInt(10));
                response.put("points", res.getInt(11));
                response.put("dislikes", res.getInt(12));
                if (related != null && Arrays.asList(related).contains("user")) {
                    args.clear();
                    args.add(res.getString("user"));
                    response.put("user", this.user_details(args));
                } else {
                    response.put("user", res.getString("user"));
                }
                if (related != null && Arrays.asList(related).contains("forum")) {

                    response.put("forum",this.forum_details(res.getString("forum"),null));
                } else {
                    response.put("forum", res.getString("forum"));
                }


            }
        } catch (SQLException r) {
            System.out.println("error");
            return null;
        }
        //System.out.println(out.toString());
        return response;

    }
    public LinkedHashMap topic_update(Integer topicId, String message, String slug) {
        ArrayList args = new ArrayList();

        args.add(0,message);
        args.add(1,slug);
        args.add(2,topicId);

        Integer id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`message`=?,t1.`slug`=? WHERE t1.`id`=?",args);

        return topic_details(topicId,null);
    }
    public LinkedHashMap topic_remove(Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        Integer id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`isDeleted`=True, t1.`posts`=0 WHERE t1.`id`=?;",args);
        Integer id2 = doSQL("UPDATE `forum_db`.`Post` as t1 SET t1.`isDeleted`=True WHERE t1.`thread`=?",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id>=0 & id2>=0) {
            resp.put("thread", id);
        }
        return resp;
    }
    public LinkedHashMap topic_restore(Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        Integer id0 = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`isDeleted`=False WHERE t1.`id`=?;",args);
        Integer id2 = doSQL("UPDATE `forum_db`.`Post` as t1 SET t1.`isDeleted`=False WHERE t1.`thread`=?",args);
        CachedRowSetImpl res= doSelect("SELECT count(id) FROM `forum_db`.Post as t1  WHERE t1.thread=? and t1.isDeleted=false ",args);
        Integer p_c=0;
        try {
          if (res.next()){
               p_c = res.getInt(1);
            }
        } catch(SQLException e) {}
        args.clear();
        args.add(0, p_c);
        args.add(1,topicId);
        Integer id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`posts`=? WHERE t1.`id`=?;",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id0>=0&id>=0&id2>=0) {
            resp.put("thread", id);
        }
        return resp;
    }

    public LinkedHashMap topic_close(Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        Integer id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`isClosed`=True WHERE t1.`id`=?;",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id>0) {
            resp.put("thread", id);
        }
        return resp;
    }

    public LinkedHashMap topic_open(Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        Integer id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`isClosed`=False WHERE t1.`id`=?;",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id>0) {
            resp.put("thread", id);
        }
        return resp;
    }

    public LinkedHashMap topic_subscribe(String user, Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        args.add(1,user);
        Integer id = doSQL("INSERT `forum_db`.`thread_followers` (`thread_id`,`user_mail`) VALUES(?,?);",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id>0) {
            resp.put("post", topicId);
            resp.put("user",user);
        }
        return resp;
    }

    public LinkedHashMap topic_unsubscribe(String user, Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        args.add(1,user);
        Integer id = doSQL("DELETE FROM `forum_db`.`thread_followers` WHERE `thread_id`=? and `user_mail`=?;",args);
        LinkedHashMap resp = new LinkedHashMap();
        if (id>0) {
            resp.put("post", topicId);
            resp.put("user",user);
        }
        return resp;
    }


    public LinkedHashMap topic_vote(Integer vote, Integer topicId) {
        ArrayList args = new ArrayList();
        args.add(0,topicId);
        Integer id=0;
        if (vote==1) {
            id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`likes`=t1.`likes`+1 WHERE t1.`id`=?;",args);
        } else {
            id = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`dislikes`=t1.`dislikes`+1 WHERE t1.`id`=?;",args);
        }
        LinkedHashMap resp = new LinkedHashMap();
        if (id>=0) {
            resp = topic_details(topicId,null);
        }
        return resp;
    }

    public ArrayList topic_list(String forum,String user,String since,Integer limit,String order) {
        ArrayList args = new ArrayList();
        ArrayList response = new ArrayList();
        args.add(0,null);
        args.add(1,since);
        args.add(2,limit);
        CachedRowSetImpl res;

        if (forum != null) {
            args.set(0, forum);
            if (order.equals("asc")) {
                res = doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes`,`likes`-`dislikes`,`dislikes`,`posts` FROM `forum_db`.`Thread` as t1 WHERE t1.`forum`=? AND t1.`date`>?  ORDER BY t1.`id`ASC LIMIT ? ", args);
            } else {
                res = doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes`,`likes`-`dislikes`,`dislikes`,`posts` FROM `forum_db`.`Thread` as t1 WHERE t1.`forum`=? AND t1.`date`>?  ORDER BY t1.`id`DESC LIMIT ? ", args);
            }
        } else {
            args.set(0, user);
            if (order.equals("asc")) {
                res = doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes`,`likes`-`dislikes`,`dislikes`,`posts` FROM `forum_db`.`Thread` as t1 WHERE t1.`user`=? AND t1.`date`>?  ORDER BY t1.`id`ASC LIMIT ? ", args);
            } else {
                res = doSelect("SELECT `id`,`forum`,`title`,`user`,`date`,`isClosed`,`isDeleted`,`message`,`slug`,`likes`,`likes`-`dislikes`,`dislikes`,`posts` FROM `forum_db`.`Thread` as t1 WHERE t1.`user`=? AND t1.`date`>?  ORDER BY t1.`id`DESC LIMIT ? ", args);
            }
        }

        try {
            while (res.next()) {
                LinkedHashMap elem = new LinkedHashMap();
                elem.put("date", res.getDate(5)+" "+res.getTime(5));
                elem.put("id", res.getInt(1));
                elem.put("posts",res.getInt(13));
                elem.put("isDeleted", (res.getString(7).equals("true")));
                elem.put("isClosed", (res.getString(6).equals("true")));
                elem.put("message", res.getString(8));
                elem.put("slug", res.getString(9));
                elem.put("title", res.getString(3));
                elem.put("likes", res.getInt(10));
                elem.put("points", res.getInt(11));
                elem.put("dislikes", res.getInt(12));
                elem.put("user", res.getString("user"));
                elem.put("forum", res.getString("forum"));
                response.add(elem);
            }
        } catch (SQLException e){
            return null;
        }

        return response;
    }

     public ArrayList topic_listPosts(Integer thread,String since,Integer limit,String order,String sort) {
         ArrayList args = new ArrayList();
         ArrayList response = new ArrayList();
         args.add(0,null);
         args.add(1,since);
         args.add(2,limit);
         CachedRowSetImpl res;
         args.set(0, thread);
         if (order.equals("asc")) {
             res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`thread`=? AND t1.`creation_date`>? ORDER BY t1.`id`ASC LIMIT ? ", args);
         } else {
             res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`thread`=? AND t1.`creation_date`>? ORDER BY t1.`id`DESC LIMIT ? ", args);
         }
         try {
             while (res.next()) {
                 LinkedHashMap elem = new LinkedHashMap();
                 elem.put("date", res.getDate(7)+" "+res.getTime(7));
                 elem.put("id", res.getInt(1));
                 elem.put("isApproved", res.getString(2).equals("true"));
                 elem.put("isHighlighted", res.getString(3).equals("true"));
                 elem.put("isEdited", res.getString(4).equals("true"));
                 elem.put("isSpam", res.getString(5).equals("true"));
                 elem.put("isDeleted", res.getString(6).equals("true"));
                 elem.put("message", res.getString(11));
                 elem.put("parent",res.getInt(12)==0?null:res.getInt(12));
                 elem.put("points",res.getInt(13));
                 elem.put("likes",res.getInt(14));
                 elem.put("dislikes",res.getInt(15));
                 elem.put("thread", res.getInt(8));
                 elem.put("user",res.getString(9));
                 elem.put("forum", res.getString(10));
                 response.add(elem);
             }
         } catch (SQLException e){
             return null;
         }

         return response;

     }

    public LinkedHashMap post_create(String date,Integer threadId, String message,String user,String forum,
                                  Integer parent, Boolean isApproved,Boolean isHighlighted,Boolean isEdited,
                                  Boolean isSpam,Boolean isDeleted) {

        LinkedHashMap resp = new LinkedHashMap();
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
        args.add(10,parent);

        int resId = doSQL("INSERT INTO `forum_db`.`Post` (`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`) VALUES (?,?,?,?,?,?,?,?,?,?,?);",args);
        if (resId > 0) {
            args.add(0,resId);
            ArrayList upd_arg = new ArrayList();
            upd_arg.add(threadId);
            int upd_count = doSQL("UPDATE `forum_db`.`Thread` as t1 SET t1.`posts`=t1.`posts`+1 WHERE t1.`id`=?;",upd_arg);
            resp.put("id", resId);
            resp.put("isApproved", isApproved);
            resp.put("isHighlighted", isHighlighted);
            resp.put("isEdited", isEdited);
            resp.put("isSpam", isSpam);
            resp.put("idDeleted", isDeleted);
            resp.put("creation_date", args.get(5));
            resp.put("thread", args.get(6));
            resp.put("user_email", args.get(7));
            resp.put("forum", args.get(8));
            resp.put("message",args.get(9));
            resp.put("parent",args.get(10));

        } else {
            System.out.println("error");
            return null;
        }

        return resp;

    }
    public LinkedHashMap post_details(Integer postId,String[] related) {
        LinkedHashMap response = new LinkedHashMap();
        ArrayList args = new ArrayList();
        args.add(0,Integer.valueOf(postId));
        CachedRowSetImpl res =  doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.id=?;",args);
        try {
            if (res.next()) {
                LinkedHashMap user = new LinkedHashMap();
                LinkedHashMap forum = new LinkedHashMap();
                response.put("date", res.getDate(7)+" "+res.getTime(7));
                response.put("id", res.getInt(1));
                response.put("isApproved", res.getString(2).equals("true"));
                response.put("isHighlighted", res.getString(3).equals("true"));
                response.put("isEdited", res.getString(4).equals("true"));
                response.put("isSpam", res.getString(5).equals("true"));
                response.put("isDeleted", res.getString(6).equals("true"));
                response.put("creation_date", res.getString(7));
                response.put("message", res.getString(11));
                response.put("parent",res.getInt(12)==0?null:res.getInt(12));
                response.put("points",res.getInt(13));
                response.put("likes",res.getInt(14));
                response.put("dislikes",res.getInt(15));

                if (related != null && Arrays.asList(related).contains("thread")) {
                    response.put("thread",this.topic_details(res.getInt(8),null));
                } else {
                    response.put("thread", res.getInt(8));
                }
                if (related != null && Arrays.asList(related).contains("user")) {
                    args.clear();
                    args.add(res.getString(9));
                    response.put("user",this.user_details(args));
                } else {
                    response.put("user", res.getString(9));
                }
                if (related != null && Arrays.asList(related).contains("forum")) {
                    response.put("forum",this.forum_details(res.getString(10),null));
                } else {
                    response.put("forum", res.getString(10));
                }

            }
        } catch (SQLException r) {
            System.out.println("error");
            return null;
        }
        //System.out.println(out.toString());
        return response;

    }
    public LinkedHashMap post_update(Integer postId, String message) {
        ArrayList args = new ArrayList();

        args.add(0,message);
        args.add(1,postId);

        Integer id = doSQL("UPDATE `forum_db`.`Post` as t1 SET t1.`message`=? WHERE t1.`id`=?",args);

        return post_details(postId,null);
    }
    public LinkedHashMap post_remove(Integer postId) {
        ArrayList args = new ArrayList();
        args.add(0,postId);
        Integer id=doSQL("UPDATE `forum_db`.Post as p SET p.isDeleted=true WHERE p.id=?",args);
        CachedRowSetImpl res = doSelect("SELECT t1.thread FROM `forum_db`.Post as t1 WHERE t1.id=?",args);
        Integer id2=0;
        try {
            if (res.next()) {
                id2=Integer.valueOf(res.getString(1));
            }
        } catch(SQLException e) {}
        LinkedHashMap resp = new LinkedHashMap();
        if (id2>=0) {
            args.clear();
            args.add(0,id2);//aka thead_id
            doSQL("UPDATE `forum_db`.Thread as t SET t.posts=t.posts-1 WHERE t.id=?",args);
            resp.put("post", postId);
        }
        return resp;
    }
    public LinkedHashMap post_restore(Integer postId) {
        ArrayList args = new ArrayList();
        args.add(0,postId);
        Integer id=doSQL("UPDATE `forum_db`.Post as p SET p.isDeleted=false WHERE p.id=?",args);
        CachedRowSetImpl res = doSelect("SELECT t1.thread FROM `forum_db`.`Post` as t1 WHERE t1.id=?",args);
        Integer id2=0;
        try {
            if (res.next()) {
                id2=Integer.valueOf(res.getString(1));
            }
        } catch(SQLException e) {}
        LinkedHashMap resp = new LinkedHashMap();
        if (id2>=0) {
            args.clear();
            args.add(0,id2);//aka thead_id
            doSQL("UPDATE `forum_db`.Thread as t SET t.posts=t.posts+1 WHERE t.id=?",args);
            resp.put("post", postId);
        }
        return resp;
    }
    public LinkedHashMap post_vote(Integer vote, Integer post) {
        ArrayList args = new ArrayList();
        args.add(0,post);
        Integer id=0;
        if (vote==1) {
            id = doSQL("UPDATE `forum_db`.`Post` as t1 SET t1.`likes`=t1.`likes`+1 WHERE t1.`id`=?;",args);
        } else {
            id = doSQL("UPDATE `forum_db`.`Post` as t1 SET t1.`dislikes`=t1.`dislikes`+1 WHERE t1.`id`=?;",args);
        }
        LinkedHashMap resp = new LinkedHashMap();
        if (id>=0) {
            resp = post_details(post,null);
        }
        return resp;
    }

    public ArrayList post_list(String forum,Integer thread,String since,Integer limit,String order) {
        ArrayList args = new ArrayList();
        ArrayList response = new ArrayList();
        args.add(0,null);
        args.add(1,since);
        args.add(2,limit);
        CachedRowSetImpl res;

            if (forum != null) {
                args.set(0, forum);
                if (order.equals("asc")) {
                    res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`forum`=? AND t1.`creation_date`>?  ORDER BY t1.`id`ASC LIMIT ? ", args);
                } else {
                    res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`forum`=? AND t1.`creation_date`>?  ORDER BY t1.`id`DESC LIMIT ? ", args);
                }
            } else {
                args.set(0, thread);
                if (order.equals("asc")) {
                    res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`thread`=? AND t1.`creation_date`>? ORDER BY t1.`id`ASC LIMIT ? ", args);
                } else {
                    res = doSelect("SELECT `id`,`isApproved`,`isHighLighted`,`isEdited`,`isSpam`,`isDeleted`,`creation_date`,`thread`,`user_email`,`forum`,`message`,`parent`,`likes`-`dislikes` as points,`likes`,`dislikes` FROM `forum_db`.`Post` as t1 WHERE t1.`thread`=? AND t1.`creation_date`>?  ORDER BY t1.`id`DESC LIMIT ? ", args);
                }
            }

        try {
            while (res.next()) {
                LinkedHashMap elem = new LinkedHashMap();
                elem.put("date", res.getDate(7)+" "+res.getTime(7));
                elem.put("id", res.getInt(1));
                elem.put("isApproved", res.getString(2).equals("true"));
                elem.put("isHighlighted", res.getString(3).equals("true"));
                elem.put("isEdited", res.getString(4).equals("true"));
                elem.put("isSpam", res.getString(5).equals("true"));
                elem.put("isDeleted", res.getString(6).equals("true"));
                elem.put("message", res.getString(11));
                elem.put("parent",res.getInt(12)==0?null:res.getInt(12));
                elem.put("points",res.getInt(13));
                elem.put("likes",res.getInt(14));
                elem.put("dislikes",res.getInt(15));
                elem.put("thread", res.getInt(8));
                elem.put("user",res.getString(9));
                elem.put("forum", res.getString(10));
                response.add(elem);
            }
        } catch (SQLException e){
            return null;
        }

        return response;
    }

}
