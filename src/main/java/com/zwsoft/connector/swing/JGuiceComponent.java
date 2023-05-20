package com.zwsoft.connector.swing;

public interface JGuiceComponent {
    void prepare();
    void display();
    default void destroy(){}
    default void prepareAndDisplay(){
        prepare();
        display();
    }
}
