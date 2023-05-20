package com.zwsoft.connector.vo;

import com.zwsoft.connector.enums.EnvPropKey;
import com.zwsoft.connector.enums.ValueType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvPropVo extends SQLiteBaseVO{
    private String env;
    private EnvPropKey key;
    private ValueType valueType;
    private String value;
}
