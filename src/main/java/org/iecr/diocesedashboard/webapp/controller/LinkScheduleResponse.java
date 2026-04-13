package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.LinkSchedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Response body for a {@link LinkSchedule}.
 *
 * @param id                  the schedule ID
 * @param serviceTemplateId   the ID of the associated service template
 * @param serviceTemplateName the human-readable name of the service template
 * @param churchNames         the list of church names included in this schedule
 * @param daysOfWeek          the days of the week on which the schedule fires
 * @param sendHour            the hour (0–23) in Costa Rica time at which the schedule fires
 * @param lastTriggeredDate   the last date on which the schedule was triggered, or null
 * @param createdAt           the timestamp when the schedule was created
 */
public record LinkScheduleResponse(
Long id,
Long serviceTemplateId,
String serviceTemplateName,
List<String> churchNames,
List<DayOfWeek> daysOfWeek,
int sendHour,
LocalDate lastTriggeredDate,
Instant createdAt) {

  /**
   * Builds a {@link LinkScheduleResponse} from a {@link LinkSchedule} entity.
   *
   * @param schedule the entity to convert
   * @return the response record
   */
  public static LinkScheduleResponse from(LinkSchedule schedule) {
    return new LinkScheduleResponse(
        schedule.getId(),
        schedule.getServiceTemplate().getId(),
        schedule.getServiceTemplate().getServiceTemplateName(),
        schedule.getChurchNames().stream().sorted().toList(),
        schedule.getDaysOfWeek().stream()
            .sorted(Comparator.comparingInt(DayOfWeek::getValue))
            .toList(),
        schedule.getSendHour(),
        schedule.getLastTriggeredDate(),
        schedule.getCreatedAt());
  }
}
