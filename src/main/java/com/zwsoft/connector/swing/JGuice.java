package com.zwsoft.connector.swing;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class JGuice {
    private static final Injector injector = Guice.createInjector(new GUIModle());

    private JGuice() {
    }

    public static <T extends JGuiceComponent> T component(Class<T> clazz){
        return injector.getInstance(clazz);
    }
}
