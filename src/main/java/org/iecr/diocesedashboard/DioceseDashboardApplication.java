package org.iecr.diocesedashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DioceseDashboardApplication {
  public static void main(String[] args) {
    SpringApplication.run(DioceseDashboardApplication.class, args);
  }
}
