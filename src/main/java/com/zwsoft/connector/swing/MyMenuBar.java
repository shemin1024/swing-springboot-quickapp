package com.zwsoft.connector.swing;

import javax.inject.Inject;
import javax.swing.*;

public class MyMenuBar extends JMenuBar implements JGuiceComponent {
    @Inject
    private EnvMenu envMenu;
    @Inject
    private BatchMenu batchMenu;
    @Override
    public void prepare() {
        this.envMenu.prepare();
        this.batchMenu.prepare();
        JMenu userMenu = new JMenu("用户");
        JMenuItem noUser = new JMenuItem("不设置");
        userMenu.add(noUser);
        this.add(userMenu);
        this.add(this.envMenu);
        this.add(this.batchMenu);
    }

    @Override
    public void display() {
        this.envMenu.display();
    }
}
