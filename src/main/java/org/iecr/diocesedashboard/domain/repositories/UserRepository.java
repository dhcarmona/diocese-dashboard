package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link DashboardUser} persistence. */
public interface UserRepository extends JpaRepository<DashboardUser, Long> {

  /**
   * Finds a user by username.
   *
   * @param username the username to search for
   * @return an {@link Optional} containing the user if found
   */
  Optional<DashboardUser> findByUsername(String username);

  /**
   * Checks whether a user with the given username already exists.
   *
   * @param username the username to check
   * @return true if a user with that username exists
   */
  boolean existsByUsername(String username);
}
