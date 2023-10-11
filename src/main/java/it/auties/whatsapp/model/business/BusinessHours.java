package it.auties.whatsapp.model.business;


import java.util.List;

/**
 * A business hours representation that contains the business' time zone and a list of business hour
 * entries.
 *
 * @param timeZone The time zone of the business.
 * @param entries  A list of business hours entries that contains information about the hours of
 *                 operation for each day of the week.
 */
public record BusinessHours(String timeZone, List<BusinessHoursEntry> entries) {

}