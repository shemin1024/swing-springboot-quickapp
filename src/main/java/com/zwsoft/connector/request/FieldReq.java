package com.zwsoft.connector.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldReq {
    private DataReq data;
    private String typeName;
    private Integer index;
    private String type;
}
