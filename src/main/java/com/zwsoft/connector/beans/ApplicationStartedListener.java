package com.zwsoft.connector.beans;

import com.zwsoft.connector.ConnectorApplication;
import com.zwsoft.connector.utils.PersistUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartedListener implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        PersistUtils.persist();
        ConnectorApplication.UIStartWorkspace();
    }
}
