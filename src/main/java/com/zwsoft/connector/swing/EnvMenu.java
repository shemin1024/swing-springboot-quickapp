package com.zwsoft.connector.swing;

import javax.inject.Inject;
import javax.swing.*;

public class EnvMenu extends JMenu implements JGuiceComponent {
    @Inject
    private ImportDialog importDialog;

    public EnvMenu() {
        super("标识注册");
    }

    @Override
    public void prepare() {
        JMenuItem manageEnv = new JMenuItem("标识导入");
        manageEnv.addActionListener((e) -> {
            this.importDialog.prepareAndDisplay();
        });
        this.add(manageEnv);
    }

    @Override
    public void display() {

    }
}
