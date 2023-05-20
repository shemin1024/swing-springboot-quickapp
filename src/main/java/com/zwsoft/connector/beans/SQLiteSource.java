package com.zwsoft.connector.beans;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SQLiteSource implements DBSource {
    private final Map<String, Connection> connectionMap = new HashMap<>();
    private final String persistDbLabel ="data.db";

    @Override
    public Connection getConnection(String label) {
        ensureConnection(label);
        return connectionMap.get(label);
    }

    @PostConstruct
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void release() {
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                Connection connection = entry.getValue();
                if (null != connection && !connection.isClosed()) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureConnection(String label) {
        Connection connection = connectionMap.get(label);
        try {
            if (null == connection || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + File.separator + label);
                connectionMap.put(label, connection);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Connection getPersistConnection() {
        return getConnection(persistDbLabel);
    }
}
