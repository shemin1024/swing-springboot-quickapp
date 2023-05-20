package com.zwsoft.connector.utils;

import com.zwsoft.connector.beans.UIGlobal;
import com.zwsoft.connector.enums.FrameState;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ContextUtils.applicationContext=applicationContext;
    }

    public static <T> T getBean(Class<T> clazz){
        if (null == applicationContext){
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    public static FrameState getFrameState(){
        UIGlobal uiGlobal = getBean(UIGlobal.class);
        if (null == uiGlobal){
            return FrameState.LOADING;
        }
        return uiGlobal.getFrameState();
    }
}
