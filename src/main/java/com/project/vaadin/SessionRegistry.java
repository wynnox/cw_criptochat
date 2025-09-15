package com.project.vaadin;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry implements VaadinServiceInitListener {

    private static final ConcurrentHashMap<String, VaadinSession> sessions = new ConcurrentHashMap<>();

    public static VaadinSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(e -> {
            VaadinSession session = e.getSession();
            sessions.put(session.getSession().getId(), session);
            log.info("New VaadinSession: {}", session.getSession().getId());
        });

        event.getSource().addSessionDestroyListener(e -> {
            sessions.remove(e.getSession().getSession().getId());
            log.info("Removed VaadinSession: {}", e.getSession().getSession().getId());
        });
    }
}
