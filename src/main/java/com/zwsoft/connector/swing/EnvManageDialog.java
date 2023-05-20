package com.zwsoft.connector.swing;

import com.zwsoft.connector.beans.WkEnv;
import com.zwsoft.connector.enums.EnvPropKey;
import com.zwsoft.connector.enums.UISignals;
import com.zwsoft.connector.signals.Payload;
import com.zwsoft.connector.signals.Signal;
import com.zwsoft.connector.utils.ContextUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EnvManageDialog extends JDialog implements JGuiceComponent {
    private JTextField jTextField;

    private JButton modifyBtn;

    private boolean prepared = false;

    public EnvManageDialog() {
        super(JGuice.component(WorkspaceFrame.class));
        setModal(true);
    }

    @Override
    public void prepare() {
        if (prepared) {
            return;
        }
        setTitle("管理环境");
        setSize(300, 200);
        setLocationRelativeTo(null);
        JPanel envListPane = new JPanel();
        Box box = Box.createVerticalBox();
        envListPane.add(box);
        jTextField = new JTextField();
        WkEnv wkEnv = ContextUtils.getBean(WkEnv.class);
        List<String> envNames = wkEnv.envNames();
        for (String en : envNames) {
            JLabel label = new JLabel(en);
            box.add(label);
            jTextField.setText(wkEnv.getStrProp(EnvPropKey.SA));
            box.add(jTextField);
        }
        add(envListPane, BorderLayout.NORTH);
        JPanel jPanel2 = new JPanel();
        modifyBtn = new JButton("修改");
        modifyBtn.addActionListener((e) -> {
            String input = jTextField.getText();
            modifyBtn.setEnabled(false);
            Signal.emit(UISignals.MODIFY_ENV, Payload.ofObject("SA", input));
        });
        jPanel2.add(modifyBtn);
        add(jPanel2, BorderLayout.SOUTH);
        prepared = true;
    }

    @Override
    public void display() {

    }

    public void close() {
    }
}
