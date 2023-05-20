package com.zwsoft.connector.swing;

import javax.inject.Inject;
import javax.swing.*;
import java.net.URL;

public class WorkspaceFrame extends JFrame implements JGuiceComponent {
    @Inject
    private DefaultWorkspacePanel defaultWorkspacePanel;
    @Inject
    private MyMenuBar myMenuBar;

    @Override
    public void prepare() {
        myMenuBar.prepare();
        setJMenuBar(myMenuBar);
        defaultWorkspacePanel.prepare();
        setTitle("标识解析与工业软件连接器 v1.0");
        setContentPane(defaultWorkspacePanel);
        setSize(480,320);
        setUndecorated(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        String defaultIcon = "/gui/ICON2.png";
        URL iconUrl = getClass().getResource(defaultIcon);
        if (null != iconUrl) {
            ImageIcon imageIcon = new ImageIcon(iconUrl);
            setIconImage(imageIcon.getImage());
            myMenuBar.display();
            defaultWorkspacePanel.display();
        }
    }
        @Override
        public void display () {
            setVisible(true);
        }

        @Override
        public void destroy () {
            JGuiceComponent.super.destroy();
        }
    }
