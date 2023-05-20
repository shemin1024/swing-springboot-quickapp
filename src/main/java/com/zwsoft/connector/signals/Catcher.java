package com.zwsoft.connector.signals;

public interface Catcher<T> {
    void connect(Catchers catchers);
    void taken(T signal,Payload payload);
}
