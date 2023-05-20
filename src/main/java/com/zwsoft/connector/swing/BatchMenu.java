package com.zwsoft.connector.swing;

import javax.inject.Inject;
import javax.swing.*;

public class BatchMenu extends JMenu implements JGuiceComponent{
    @Inject
    private BatchImportDialog importDialog;
    public BatchMenu() {
        super("标识批量注册");
    }

    @Override
    public void prepare() {
        JMenuItem manageEnv = new JMenuItem("标识批量导入");
        manageEnv.addActionListener((e) -> {
            this.importDialog.prepareAndDisplay();
        });
        this.add(manageEnv);
    }

    @Override
    public void display() {

    }
}
