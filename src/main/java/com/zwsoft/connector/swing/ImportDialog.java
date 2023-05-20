package com.zwsoft.connector.swing;


import com.zwsoft.connector.enums.UISignals;
import com.zwsoft.connector.signals.Payload;
import com.zwsoft.connector.signals.Signal;
import com.zwsoft.connector.vo.AttrNameValueVo;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
@Singleton
public class ImportDialog extends JDialog implements JGuiceComponent {
    private boolean prepared = false;

    private JButton importBtn;

    private JButton registerBtn;

    private JTable resultArea;

    private JTextArea textArea;

    private List<AttrNameValueVo> partVoList;

    public ImportDialog() {
        super(JGuice.component(WorkspaceFrame.class));
        setModal(false);
        setTitle("标识导入");
        setSize(650, 480);
    }

    @Override
    public void prepare() {
        if (prepared) {
            return;
        }
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        Box topBarBox = Box.createHorizontalBox();
        topPanel.add(topBarBox);
        topBarBox.add(Box.createHorizontalStrut(20));
        importBtn = new JButton("import!");
        importBtn.addActionListener((e) -> {
            importBtn.setEnabled(false);
            Signal.emit(UISignals.IMPORT);
        });
        registerBtn = new JButton("register!");
        registerBtn.setEnabled(false);
        registerBtn.addActionListener((e) -> {
            registerBtn.setEnabled(false);
            Signal.emit(UISignals.REGISTER, Payload.ofObject("data", partVoList));
        });
        topBarBox.add(importBtn);
        topBarBox.add(registerBtn);
        contentPanel.add(topPanel, BorderLayout.NORTH);
        setContentPane(contentPanel);
        String[] columnNames = {"AttributeName", "AttributeValue"};
        Object[][] data = new Object[0][0];
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        resultArea = new JTable(tableModel);
        JScrollPane resultScrollPanel = new JScrollPane(resultArea);
        resultScrollPanel.setSize(300, 200);
        contentPanel.add(resultScrollPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        textArea = new JTextArea();
        textArea.setTabSize(2);
        contentPanel.add(textArea, BorderLayout.SOUTH);
        prepared = true;
    }

    @Override
    public void display() {
        setVisible(true);
    }

    public void importResult(List<AttrNameValueVo> objectVoList) {
        this.partVoList = objectVoList;
        String[] columnNames = new String[]{"name", "value"};
        Object[][] objects = new Object[objectVoList.size()][2];

        for(int i = 0; i < objectVoList.size(); ++i) {
            objects[i][0] = objectVoList.get(i).getName();
            objects[i][1] = objectVoList.get(i).getValue();
        }

        DefaultTableModel defaultTableModel = (DefaultTableModel)this.resultArea.getModel();
        defaultTableModel.setDataVector(objects, columnNames);
        defaultTableModel.fireTableStructureChanged();
        this.textArea.setText("");
        this.importBtn.setEnabled(true);
        this.registerBtn.setEnabled(true);
    }

    public void registerResult(String id,String res) {
        String[] columnNames = new String[]{"name", "value"};
        List<AttrNameValueVo> objectVoList = new ArrayList<>();
        objectVoList.add(new AttrNameValueVo("Identifier",id));
        Object[][] objects = new Object[objectVoList.size()][2];
        for(int i = 0; i < objectVoList.size(); ++i) {
            objects[i][0] = objectVoList.get(i).getName();
            objects[i][1] = objectVoList.get(i).getValue();
        }
        DefaultTableModel defaultTableModel = (DefaultTableModel)this.resultArea.getModel();
        defaultTableModel.setDataVector(objects, columnNames);
        defaultTableModel.fireTableStructureChanged();
        this.textArea.setText(res);
        this.importBtn.setEnabled(true);
        this.registerBtn.setEnabled(true);
    }

}
