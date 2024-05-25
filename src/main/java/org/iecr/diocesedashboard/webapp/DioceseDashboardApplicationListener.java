package org.iecr.diocesedashboard.webapp;

import java.util.logging.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class DioceseDashboardApplicationListener implements
    ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOG
      = Logger.getLogger(DioceseDashboardApplicationListener.class.getName());

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    LOG.info("Application Loaded");
  }
}
