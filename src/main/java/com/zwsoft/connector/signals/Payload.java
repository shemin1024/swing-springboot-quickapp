package com.zwsoft.connector.signals;

import java.util.HashMap;

public class Payload extends HashMap<String,Object> {
    public static Payload ofObject(String key, Object val){
        Payload payload = new Payload();
        payload.put(key,val);
        return payload;
    }
}
