package org.iecr.diocesedashboard.webapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.service.LinkScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ScheduledLinkRunnerTest {

  @Mock
  private LinkScheduleService linkScheduleService;

  private ScheduledLinkRunner runner;

  @BeforeEach
  void setUp() {
    runner = new ScheduledLinkRunner(linkScheduleService, "https://example.com");
  }

  private LinkSchedule scheduleFor(int sendHour, DayOfWeek... days) {
    LinkSchedule schedule = new LinkSchedule();
    schedule.setId(1L);
    schedule.setSendHour(sendHour);
    schedule.setDaysOfWeek(Set.of(days));
    schedule.setChurchNames(Set.of("Church A"));
    return schedule;
  }

  @Test
  void runSchedules_executesMatchingSchedule() {
    ZonedDateTime nowCr = ZonedDateTime.now(ScheduledLinkRunner.COSTA_RICA_ZONE);
    int hour = nowCr.getHour();
    DayOfWeek day = nowCr.getDayOfWeek();

    LinkSchedule schedule = scheduleFor(hour, day);
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule));

    runner.runSchedules();

    verify(linkScheduleService).executeSchedule(schedule, "https://example.com");
  }

  @Test
  void runSchedules_skips_whenHourDoesNotMatch() {
    ZonedDateTime nowCr = ZonedDateTime.now(ScheduledLinkRunner.COSTA_RICA_ZONE);
    int wrongHour = (nowCr.getHour() + 1) % 24;
    DayOfWeek day = nowCr.getDayOfWeek();

    LinkSchedule schedule = scheduleFor(wrongHour, day);
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule));

    runner.runSchedules();

    verify(linkScheduleService, never()).executeSchedule(any(), any());
  }

  @Test
  void runSchedules_skips_whenDayOfWeekDoesNotMatch() {
    ZonedDateTime nowCr = ZonedDateTime.now(ScheduledLinkRunner.COSTA_RICA_ZONE);
    int hour = nowCr.getHour();
    DayOfWeek wrongDay = nowCr.getDayOfWeek().plus(1);

    LinkSchedule schedule = scheduleFor(hour, wrongDay);
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule));

    runner.runSchedules();

    verify(linkScheduleService, never()).executeSchedule(any(), any());
  }

  @Test
  void runSchedules_skips_whenAlreadyTriggeredToday() {
    ZonedDateTime nowCr = ZonedDateTime.now(ScheduledLinkRunner.COSTA_RICA_ZONE);
    int hour = nowCr.getHour();
    DayOfWeek day = nowCr.getDayOfWeek();
    LocalDate today = nowCr.toLocalDate();

    LinkSchedule schedule = scheduleFor(hour, day);
    schedule.setLastTriggeredDate(today);
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule));

    runner.runSchedules();

    verify(linkScheduleService, never()).executeSchedule(any(), any());
  }

  @Test
  void runSchedules_continuesOnError_whenOneScheduleFails() {
    ZonedDateTime nowCr = ZonedDateTime.now(ScheduledLinkRunner.COSTA_RICA_ZONE);
    int hour = nowCr.getHour();
    DayOfWeek day = nowCr.getDayOfWeek();

    LinkSchedule schedule1 = scheduleFor(hour, day);
    schedule1.setId(1L);
    LinkSchedule schedule2 = scheduleFor(hour, day);
    schedule2.setId(2L);
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule1, schedule2));
    when(linkScheduleService.findAll()).thenReturn(List.of(schedule1, schedule2));

    Mockito.doThrow(new RuntimeException("Twilio down"))
        .when(linkScheduleService).executeSchedule(eq(schedule1), any());

    runner.runSchedules();

    verify(linkScheduleService, times(1)).executeSchedule(schedule1, "https://example.com");
    verify(linkScheduleService, times(1)).executeSchedule(schedule2, "https://example.com");
  }
}
