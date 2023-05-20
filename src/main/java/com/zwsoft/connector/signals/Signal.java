package com.zwsoft.connector.signals;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Signal implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public static <T> void emit(T t){emit(t,new Payload());}

    public static <T> void emit(T t,Payload payload){
        Signal.applicationContext.getBean(Catchers.class).taken(t,payload);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Signal.applicationContext = applicationContext;
    }
}
