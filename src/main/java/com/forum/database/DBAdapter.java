package com.forum.database;

import com.mysql.fabric.jdbc.FabricMySQLDriver;

import java.sql.*;

/**
 * Created by mid on 29.10.14.
 */
public class DBAdapter {
    private static final String URL = "jdbc:mysql://localhost:3306";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1234QWer";

    public DBAdapter() {

    }
    public void clear() {//очистить БД
        int i = 0;
        try {
            Driver driver = new FabricMySQLDriver();
            DriverManager.registerDriver(driver);

        } catch (SQLException e) {
            System.err.println("Не удалось загрузить драйвер !!!");
        }
        try(Connection conection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            Statement statement = conection.createStatement()) {
            ResultSet res;
            //res = statement.executeQuery("CREATE DATABASE  IF NOT EXISTS `forum_db` /*!40100 DEFAULT CHARACTER SET latin1 */;\n" +
            //       "USE `forum_db`;");
           // res = statement.executeQuery("DROP TABLE IF EXISTS `forum_db`.`Forum`");

            //TODO TRUNCATE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            statement.execute("TRUNCATE TABLE  `forum_db`.`Forum`");
            statement.execute("TRUNCATE TABLE forum_db.`Post`");
            statement.execute("TRUNCATE TABLE forum_db.`Thread`");
            statement.execute("TRUNCATE TABLE forum_db.`User`");
            //statement.execute("SET character_set_client = utf8");
            //statement.execute("CREATE TABLE forum_db.`Forum` (   `name` varchar(100) COLLATE utf8_bin NOT NULL,   `short_name` varchar(100) COLLATE utf8_bin NOT NULL,   `user_mail` varchar(45) COLLATE utf8_bin NOT NULL,   PRIMARY KEY (`user_mail`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin");
            //statement.execute("CREATE TABLE forum_db.`Post` (   `id` int(11) NOT NULL AUTO_INCREMENT,   `level` int(11) DEFAULT NULL,   `isApproved` bit(1) DEFAULT b'0',   `isHithLighted` bit(1) DEFAULT b'0',   `isEdited` bit(1) DEFAULT b'0',   `isSpam` bit(1) DEFAULT b'0',   `isDeleted` bit(1) DEFAULT b'0',   `creation_date` datetime NOT NULL,   `thread_id` int(11) NOT NULL,   `user_email` varchar(50) COLLATE utf8_bin NOT NULL,   `forum_id` int(11) NOT NULL,   `message` varchar(500) COLLATE utf8_bin NOT NULL,   PRIMARY KEY (`id`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin");
            //statement.execute("CREATE TABLE forum_db.`Thread` (   `id` int(11) NOT NULL AUTO_INCREMENT,   `forum` varchar(100) COLLATE utf8_bin NOT NULL,   `title` varchar(200) COLLATE utf8_bin NOT NULL,   `user` varchar(50) COLLATE utf8_bin NOT NULL,   `creation_date` datetime NOT NULL,   `isClosed` bit(1) DEFAULT b'0',   `isDeleted` bit(1) DEFAULT b'0',   `message` varchar(500) COLLATE utf8_bin NOT NULL,   `slug` varchar(200) COLLATE utf8_bin DEFAULT NULL,   PRIMARY KEY (`id`) ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin");
            //statement.execute("CREATE TABLE forum_db.`User` (   `email` varchar(50) COLLATE utf8_bin NOT NULL,   `name` varchar(50) COLLATE utf8_bin DEFAULT NULL,   `about` varchar(200) COLLATE utf8_bin DEFAULT NULL,   `user_name` varchar(45) COLLATE utf8_bin NOT NULL,   PRIMARY KEY (`email`) ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin");
        } catch (SQLException e) {
            System.err.println("Ошибка");
        }

    }
}
