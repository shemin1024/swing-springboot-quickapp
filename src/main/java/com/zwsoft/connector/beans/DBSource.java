package com.zwsoft.connector.beans;

import java.sql.Connection;

public interface DBSource {
    Connection getConnection(String label);
}
