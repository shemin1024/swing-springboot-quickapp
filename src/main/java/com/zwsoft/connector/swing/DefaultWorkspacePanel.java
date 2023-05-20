package com.zwsoft.connector.swing;

import javax.inject.Inject;
import javax.swing.*;

public class DefaultWorkspacePanel extends JPanel implements JGuiceComponent {

    @Override
    public void prepare() {
        Box box = Box.createVerticalBox();
        add(box);
        Box box1 = Box.createHorizontalBox();
        box.add(box1);
        box1.add(Box.createHorizontalStrut(40));

    }

    @Override
    public void display() {

    }
}
