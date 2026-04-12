package org.iecr.diocesedashboard.webapp;

import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.service.LinkScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Scheduled component that fires at the top of every UTC hour and executes any
 * {@link LinkSchedule}s whose configured send hour (in Costa Rica time, UTC-6) matches
 * the current hour in that timezone and that have not yet been triggered today.
 */
@Component
public class ScheduledLinkRunner {

  // Costa Rica does not observe DST; it is always UTC-6.
  static final ZoneId COSTA_RICA_ZONE = ZoneId.of("America/Costa_Rica");

  private static final Logger logger = LoggerFactory.getLogger(ScheduledLinkRunner.class);

  private final LinkScheduleService linkScheduleService;
  private final String baseUrl;

  @Autowired
  public ScheduledLinkRunner(LinkScheduleService linkScheduleService,
      @Value("${app.base-url}") String baseUrl) {
    this.linkScheduleService = linkScheduleService;
    this.baseUrl = baseUrl;
  }

  /**
   * Runs at the top of every UTC hour. Converts the current instant to Costa Rica local time,
   * then fires any schedules that match the current day-of-week and hour and have not yet
   * been triggered today (in Costa Rica local date).
   */
  @Scheduled(cron = "0 0 * * * *")
  public void runSchedules() {
    ZonedDateTime nowCr = ZonedDateTime.now(COSTA_RICA_ZONE);
    int currentHour = nowCr.getHour();
    DayOfWeek currentDay = nowCr.getDayOfWeek();
    LocalDate today = nowCr.toLocalDate();

    List<LinkSchedule> schedules = linkScheduleService.findAll();
    logger.debug("Checking {} schedule(s) at CR time {}:00 on {}",
        schedules.size(), currentHour, currentDay);

    for (LinkSchedule schedule : schedules) {
      if (schedule.getSendHour() != currentHour) {
        continue;
      }
      if (!schedule.getDaysOfWeek().contains(currentDay)) {
        continue;
      }
      if (today.equals(schedule.getLastTriggeredDate())) {
        logger.debug("Schedule {} already triggered today, skipping.", schedule.getId());
        continue;
      }
      try {
        linkScheduleService.executeSchedule(schedule, baseUrl);
      } catch (Exception ex) {
        logger.error("Failed to execute schedule {}: {}", schedule.getId(), ex.getMessage(), ex);
      }
    }
  }
}
