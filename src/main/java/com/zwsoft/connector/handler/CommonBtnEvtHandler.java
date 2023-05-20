package com.zwsoft.connector.handler;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zwsoft.connector.beans.HttpClient;
import com.zwsoft.connector.beans.WkEnv;
import com.zwsoft.connector.enums.FieldNameEnum;
import com.zwsoft.connector.enums.UISignals;
import com.zwsoft.connector.request.DataReq;
import com.zwsoft.connector.request.FieldReq;
import com.zwsoft.connector.request.RegisterReq;
import com.zwsoft.connector.signals.Catcher;
import com.zwsoft.connector.signals.Catchers;
import com.zwsoft.connector.signals.Payload;
import com.zwsoft.connector.swing.BatchImportDialog;
import com.zwsoft.connector.swing.EnvManageDialog;
import com.zwsoft.connector.swing.ImportDialog;
import com.zwsoft.connector.swing.JGuice;
import com.zwsoft.connector.utils.ExcelUtils;
import com.zwsoft.connector.vo.AttrNameValueVo;
import com.zwsoft.connector.vo.BatchDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.*;

@Component
@Slf4j
public class CommonBtnEvtHandler implements Catcher<UISignals> {

    @Autowired
    private HttpClient wkRpc;
    @Autowired
    private WkEnv wkEnv;

    private static final String PREFIX = "88.199.604";
    private static final String TEMPLATE = "ZW-CAD";

    public CommonBtnEvtHandler() {
    }

    @Override
    public void connect(Catchers catchers) {
        Set<UISignals> signals = new HashSet<>();
        signals.add(UISignals.IMPORT);
        signals.add(UISignals.REGISTER);
        signals.add(UISignals.MODIFY_ENV);
        signals.add(UISignals.BATCH_IMPORT);
        signals.add(UISignals.BATCH_REGISTER);
        catchers.join(this, signals);
    }


    @Override
    public void taken(UISignals signal, Payload payload) {
        if (UISignals.IMPORT.equals(signal)) {
            dealImport();
        } else if (UISignals.REGISTER.equals(signal)) {
            dealRegister((List<AttrNameValueVo>) payload.get("data"));
        } else if (UISignals.MODIFY_ENV.equals(signal)) {
            dealModifyEnv((String) payload.get("SA"));
        } else if (UISignals.BATCH_IMPORT.equals(signal)) {
            dealBatchImport();
        } else if (UISignals.BATCH_REGISTER.equals(signal)) {
            dealBatchRegister((List<BatchDataVo>) payload.get("data"));
        }

    }

    private void dealBatchRegister(List<BatchDataVo> data) {
        String serverIp = "http://82.157.40.213:8124";
        String uri = serverIp + "/identifier/saveIdentifier";
        String res = "";
        for (BatchDataVo partVo : data) {
            RegisterReq registerReq = new RegisterReq();
            String uuid = UUID.randomUUID().toString();
            String handler = PREFIX + "/" + uuid;
            registerReq.setHandle(handler);
            registerReq.setTemplateVersion(TEMPLATE);
            List<FieldReq> fieldReqs = new ArrayList<>();
            for (FieldNameEnum kind : FieldNameEnum.values()
            ) {
                FieldReq fieldReq = new FieldReq();
                DataReq dataReq = new DataReq();
                dataReq.setFormat("string");
                fieldReq.setIndex(kind.getIndex());
                fieldReq.setType(kind.name());
                fieldReq.setTypeName(kind.getCnName());
                switch (kind) {
                    case PartName:
                        dataReq.setValue(partVo.getPartName());
                        break;
                    case ProductNumber:
                        dataReq.setValue(partVo.getProductNumber());
                        break;
                    case SupplierName:
                        dataReq.setValue(partVo.getSupplierName());
                        break;
                    case PartClass:
                        dataReq.setValue(partVo.getPartClass());
                        break;
                }
                fieldReq.setData(dataReq);
                fieldReqs.add(fieldReq);
            }
            registerReq.setValue(fieldReqs);
            JSONObject jo = wkRpc.postWithBody(uri, registerReq);
            res = JSON.toJSONString(jo.getString("msg"), true);
        }
        String finalRes = res;
        EventQueue.invokeLater(() -> {
            JGuice.component(BatchImportDialog.class).registerResult(finalRes);
        });
    }

    private void dealBatchImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.showOpenDialog(null);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return null;
            }
        });
        File file = fileChooser.getSelectedFile();
        if (ObjectUtil.isNull(file)) {
            return;
        }
        List<BatchDataVo> dataList = new ArrayList<>();
        try {
            dataList = ExcelUtils.readExcel(BatchDataVo.class, new FileInputStream(file));
            System.out.println(dataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<BatchDataVo> finalDataList = dataList;
        EventQueue.invokeLater(() -> {
            JGuice.component(BatchImportDialog.class).importResult(finalDataList);
        });
    }

    private void dealModifyEnv(String sa) {
        wkEnv.writeEnv(sa);
        EventQueue.invokeLater(() -> {
            JGuice.component(EnvManageDialog.class).close();
        });
    }

    private void dealRegister(List<AttrNameValueVo> data) {
        String serverIp = "http://82.157.40.213:8124";
        String uri = serverIp + "/identifier/saveIdentifier";
        RegisterReq registerReq = new RegisterReq();
        String uuid = UUID.randomUUID().toString();
        String handler = PREFIX + "/" + uuid;
        registerReq.setHandle(handler);
        registerReq.setTemplateVersion(TEMPLATE);
        List<FieldReq> fieldReqs = new ArrayList<>();
        for (AttrNameValueVo partVo : data) {
            FieldReq fieldReq = new FieldReq();
            DataReq dataReq = new DataReq();
            String name = partVo.getName();
            dataReq.setFormat("string");
            dataReq.setValue(partVo.getValue());
            if ("Identifier".equalsIgnoreCase(name)) {
                continue;
            }
            FieldNameEnum fieldNameEnum = EnumUtil.fromString(FieldNameEnum.class, name);
            fieldReq.setIndex(fieldNameEnum.getIndex());
            fieldReq.setType(fieldNameEnum.name());
            fieldReq.setTypeName(fieldNameEnum.getCnName());
            fieldReq.setData(dataReq);
            fieldReqs.add(fieldReq);
        }
        registerReq.setValue(fieldReqs);
        JSONObject jo = wkRpc.postWithBody(uri, registerReq);
        String res = JSON.toJSONString(jo.getString("msg"), true);
        EventQueue.invokeLater(() -> {
            JGuice.component(ImportDialog.class).registerResult(handler, res);
        });
    }

    private void dealImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.showOpenDialog(null);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return null;
            }
        });
        File file = fileChooser.getSelectedFile();
        if (ObjectUtil.isNull(file)) {
            return;
        }
        List<AttrNameValueVo> dataList = new ArrayList<>();
        try {
            dataList = ExcelUtils.readExcel(AttrNameValueVo.class, new FileInputStream(file));
            System.out.println(dataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<AttrNameValueVo> finalDataList = dataList;
        EventQueue.invokeLater(() -> {
            JGuice.component(ImportDialog.class).importResult(finalDataList);
        });
    }

}

