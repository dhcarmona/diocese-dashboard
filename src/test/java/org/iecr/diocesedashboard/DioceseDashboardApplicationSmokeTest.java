package org.iecr.diocesedashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
    classes = DioceseDashboardApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:startuptest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "dashboard.bootstrap-admin.enabled=false"
    })
class DioceseDashboardApplicationSmokeTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads_fullApplicationStartsSuccessfully() {
    assertThat(applicationContext).isNotNull();
    assertThat(applicationContext.containsBean("securityConfig")).isTrue();
    assertThat(applicationContext.containsBean("userService")).isTrue();
    assertThat(applicationContext.containsBean("bootstrapAdminInitializer")).isTrue();
  }
}
