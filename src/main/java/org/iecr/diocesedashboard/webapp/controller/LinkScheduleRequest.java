package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.iecr.diocesedashboard.domain.objects.LinkSchedule;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Request body for creating or updating a {@link LinkSchedule}.
 *
 * @param serviceTemplateId the ID of the service template to use
 * @param churchNames       the names of the churches to create links for
 * @param daysOfWeek        the days of the week on which the schedule fires
 * @param sendHour          the hour (0–23) in Costa Rica time (America/Costa_Rica, UTC-6)
 *                          at which the schedule fires
 */
public record LinkScheduleRequest(
@NotNull Long serviceTemplateId,
@NotEmpty List<String> churchNames,
@NotEmpty List<DayOfWeek> daysOfWeek,
@Min(0) @Max(23) int sendHour) {
}
