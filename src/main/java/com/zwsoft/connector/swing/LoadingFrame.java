package com.zwsoft.connector.swing;

import com.zwsoft.connector.enums.FrameState;
import com.zwsoft.connector.utils.ContextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.net.URL;
@Slf4j
public class LoadingFrame extends JFrame implements JGuiceComponent {
    private boolean prepared = false;

    public LoadingFrame() {
        setSize(320, 240);
        setUndecorated(true);
        setLocationRelativeTo(null);
        String defaultIcon = "/gui/ICON2.png";
        URL iconUrl = getClass().getResource(defaultIcon);
        if (null != iconUrl) {
            ImageIcon imageIcon = new ImageIcon(iconUrl);
            setIconImage(imageIcon.getImage());
        }
    }

    @Override
    public void prepare() {
        if (prepared) {
            return;
        }
        DefaultStyledDocument document = new DefaultStyledDocument();
        Color bg = getBackground();
        Color fg = Color.DARK_GRAY;
        JTextPane loadingPane = new JTextPane(document);
        loadingPane.setBackground(bg);
        loadingPane.setEditable(false);
        loadingPane.setHighlighter(null);
        StyleContext sc = new StyleContext();
        try {
            Style s1 = sc.addStyle("chinese-part", null);
            StyleConstants.setBackground(s1, bg);
            StyleConstants.setForeground(s1, fg);
            StyleConstants.setFontFamily(s1, "华文行楷");
            StyleConstants.setFontSize(s1, 24);
            document.insertString(0, "加载中，请稍后", s1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
        document.setParagraphAttributes(0, document.getLength(), attributeSet, false);
        JPanel jp = new JPanel();
        Box box = Box.createVerticalBox();
        jp.add(box);
        jp.add(Box.createHorizontalStrut(320));
        jp.add(Box.createVerticalStrut(200));
        jp.add(loadingPane);
        jp.add(Box.createVerticalGlue());
        setContentPane(jp);
        prepared = true;
    }

    @Override
    public void display() {
        setVisible(true);
        EventQueue.invokeLater(() -> {
            FrameState state = ContextUtils.getFrameState();
            if (FrameState.LOADING.equals(state)) {
                long ms = System.currentTimeMillis();
                while (true) {
                    if (ms + 60000L < System.currentTimeMillis()) {
                        log.error("Load timeout,shutdown");
                        System.exit(1);
                    }
                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        log.error("Failed to load");
                        e.printStackTrace();
                        System.exit(2);
                    }
                    state = ContextUtils.getFrameState();
                    if (FrameState.WORKSPACE.equals(state)) {
                        destroy();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void destroy() {
        dispose();
    }
}
