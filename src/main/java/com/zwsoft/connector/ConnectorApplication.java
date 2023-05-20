package com.zwsoft.connector;

import com.zwsoft.connector.beans.UIGlobal;
import com.zwsoft.connector.enums.FrameState;
import com.zwsoft.connector.swing.JGuice;
import com.zwsoft.connector.swing.LoadingFrame;
import com.zwsoft.connector.swing.WorkspaceFrame;
import com.zwsoft.connector.utils.ContextUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;

@SpringBootApplication
public class ConnectorApplication {

    public static void main(String[] args) {
        EventQueue.invokeLater(ConnectorApplication::UIPrepare);
        SpringApplication.run(ConnectorApplication.class, args);
    }

    private static void UIPrepare() {
        LoadingFrame loadingFrame = JGuice.component(LoadingFrame.class);
        loadingFrame.prepare();
        loadingFrame.display();
    }

    public static void UIStartWorkspace(){
        WorkspaceFrame workspaceFrame = JGuice.component(WorkspaceFrame.class);
        workspaceFrame.prepareAndDisplay();
        UIGlobal uiGlobal = ContextUtils.getBean(UIGlobal.class);
        if (null ==uiGlobal){
            throw new RuntimeException("not prepared to start workspace");
        }
        uiGlobal.setFrameState(FrameState.WORKSPACE);
    }

}
