package com.zwsoft.connector.signals;

import com.zwsoft.connector.enums.UISignals;
import com.zwsoft.connector.handler.CommonBtnEvtHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class Catchers implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Map<Object, Set<Catcher<?>>> catcherGroup = new HashMap<>();
    private ReentrantLock locker = new ReentrantLock();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initCatchers() {
        locker.lock();
        try {
            Map<String, Catcher> catcherMap = applicationContext.getBeansOfType(Catcher.class);
            for (Map.Entry<String, Catcher> entry : catcherMap.entrySet()
            ) {
                entry.getValue().connect(this);
            }
        } catch (BeansException e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }
    }

    public <T> void taken(T signal, Payload payload) {
        locker.lock();
        try {
            if (catcherGroup.containsKey(signal)) {
                Set<Catcher<?>> catchers = catcherGroup.get(signal);
                for (Catcher catcher : catchers
                ) {
                    catcher.taken(signal, payload);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }
    }

    public void join(CommonBtnEvtHandler commonBtnEvtHandler, Set<UISignals> signals) {
        this.locker.lock();

        Object signal;
        for(Iterator var3 = signals.iterator(); var3.hasNext(); this.catcherGroup.get(signal).add(commonBtnEvtHandler)) {
            signal = var3.next();
            if (!this.catcherGroup.containsKey(signal)) {
                this.catcherGroup.put(signal, new HashSet());
            }
        }

        this.locker.unlock();
    }

    public void disconnectAll(CommonBtnEvtHandler commonBtnEvtHandler) {
    }
}
