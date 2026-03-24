package org.iecr.diocesedashboard.webapp;

import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to populate a {@link org.springframework.security.core.context.SecurityContext}
 * with a {@link DashboardUserDetails} principal for use in MockMvc tests.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = MockDashboardUserSecurityContextFactory.class)
public @interface WithMockDashboardUser {

  /** The role to assign to the mock user. Defaults to ADMIN. */
  UserRole role() default UserRole.ADMIN;

  /** A single church name to assign. Defaults to empty (no assigned church). */
  String churchName() default "";

  /** Multiple church names to assign to the mock user. */
  String[] churchNames() default {};
}
