package com.zwsoft.connector.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersistVO extends SQLiteBaseVO{

    private String key;
    private Integer idx;
    private String val;
}
