package net.whydah.identity.admin.innsyn;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InnsynReportTest {

    private static final long AFTERNOON = ZonedDateTime.of(2026, 10, 14, 14, 5, 9, 0, InnsynReport.ZONE).toInstant().toEpochMilli();

    private static String row(long time, String function, String appId) {
        return "{\"prefix\":\"\",\"name\":\"usersession\",\"startTime\":" + time + ",\"data\":{\"usersessionfunction\":\""
                + function + "\",\"applicationid\":\"" + appId + "\",\"userid\":\"u1\",\"applicationtokenid\":\"t\"}}";
    }

    private static String stats(String... rows) {
        return "{\"prefix\":\"All\",\"activityName\":\"userSession\",\"activities\":{\"userSessions\":[" + String.join(",", rows) + "]}}";
    }

    private static InnsynReport report(InnsynReport.Source crm, List<InnsynReport.Source> sessions) {
        return new InnsynReport("uid-1", "40000000", crm, sessions, Map.of("2215", "1881 App"),
                LocalDate.of(2026, 9, 13), LocalDate.of(2026, 10, 15), Instant.ofEpochMilli(AFTERNOON));
    }

    @Test
    void duplicateEventsFromSeveralStsNodesAreShownOnce() {
        String json = stats(row(AFTERNOON, "userSessionCreatedByPin", "2215"),
                row(AFTERNOON, "userSessionCreatedByPin", "2215"),
                row(AFTERNOON, "userSessionCreatedByPin", "2215"));
        assertEquals(1, InnsynReport.parseSessions(List.of(InnsynReport.Source.ok(json))).size());
    }

    @Test
    void timesUseThe24HourClockInNorwegianTime() {
        String html = report(null, List.of(InnsynReport.Source.ok(stats(row(AFTERNOON, "userSessionCreatedByPin", "2215"))))).render();
        assertTrue(html.contains("2026-10-14 14:05:09"), html);
        assertTrue(html.contains("Innlogging med engangskode (SMS)"));
        assertTrue(html.contains("1881 App (2215)"));
    }

    @Test
    void routineSessionUseIsCountedPerDayAndListedInTheAppendix() {
        String html = report(null, List.of(InnsynReport.Source.ok(stats(
                row(AFTERNOON, "userSessionAccess", "2215"),
                row(AFTERNOON + 1000, "userSessionAccess", "2215"),
                row(AFTERNOON + 2000, "userSessionVerification", "2215"))))).render();
        assertTrue(html.contains("<td>2026-10-14</td><td>1881 App (2215)</td><td>3</td>"), html);
        assertTrue(html.contains("Vedlegg: alle registrerte sesjonshendelser"));
        assertTrue(html.contains("Ingen innlogginger eller kontohendelser er registrert i perioden."));
    }

    @Test
    void aFailedLookupIsReportedAndNotShownAsNoData() {
        String html = report(InnsynReport.Source.failed("kundetjenesten svarte ikke (status 502)"),
                List.of(InnsynReport.Source.failed("statistikktjenesten svarte ikke i tide"))).render();
        assertTrue(html.contains("Kundeopplysningene kunne ikke hentes"));
        assertTrue(html.contains("Sesjonshistorikken kunne ikke hentes fullstendig"));
        assertFalse(html.contains("Ingen innlogginger eller kontohendelser er registrert"));
        assertFalse(html.contains("Ingen bruk av innlogget sesjon"));
    }

    @Test
    void everyCrmFieldIsShownAndHtmlIsEscaped() {
        String crm = "{\"firstname\":\"Ola <b>\",\"middlename\":null,\"emailaddresses\":{\"privat\":{\"emailaddress\":\"ola@example.com\"}},"
                + "\"deliveryaddresses\":[{\"addressLine1\":\"Gate 1\"}]}";
        String html = report(InnsynReport.Source.ok(crm), List.of(InnsynReport.Source.ok(stats()))).render();
        assertTrue(html.contains("<td>firstname</td><td>Ola &lt;b&gt;</td>"), html);
        assertTrue(html.contains("<td>emailaddresses.privat.emailaddress</td><td>ola@example.com</td>"));
        assertTrue(html.contains("<td>deliveryaddresses[1].addressLine1</td><td>Gate 1</td>"));
        assertFalse(html.contains("middlename"));
    }

    @Test
    void noCrmProfileIsStatedExplicitly() {
        String html = report(null, List.of(InnsynReport.Source.ok(stats()))).render();
        assertTrue(html.contains("Det er ikke registrert en kundeprofil for denne brukeren."));
    }
}
