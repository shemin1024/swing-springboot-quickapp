package com.zwsoft.connector.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@Getter
@Setter
public class SQLiteBaseVO {
    private Long id;

    private Long v;
    private Date ctime;
    private Date utime;
}
