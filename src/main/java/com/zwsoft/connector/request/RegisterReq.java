package com.zwsoft.connector.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterReq {
    private String handle;
    private String templateVersion;
    private List<FieldReq> value;
}
