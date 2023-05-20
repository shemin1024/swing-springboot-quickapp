package com.zwsoft.connector.utils;

import com.zwsoft.connector.vo.SQLiteBaseVO;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {
    private ReflectUtils(){}

    public static Object getFieldValue(Object obj,String fieldName){
        Object value = null;
        String getter = "get"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1);
        try {
            Method getterMethod = obj.getClass().getMethod(getter);
            value = getterMethod.invoke(obj);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static <T extends SQLiteBaseVO> void setFieldValue(T item, String fieldName, Object v) {
        String setter = "set"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1);
        try {
            Method getterMethod = item.getClass().getMethod(setter);
            getterMethod.invoke(item,v);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
