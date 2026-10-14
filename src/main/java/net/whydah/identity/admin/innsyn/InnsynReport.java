package net.whydah.identity.admin.innsyn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Printable HTML report for a GDPR access request (innsynsbegjæring, Art. 15) about one user: the CRM customer
 * record and the session history kept by the statistics service. An admin opens it from the user list, saves it
 * as PDF and sends it to the person.
 *
 * A source that could not be read is reported as such. An empty section must only ever mean "no data".
 */
public class InnsynReport {

    static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // routine use of an existing session; summarised per day, listed in full in the appendix
    private static final Set<String> ROUTINE = Set.of("userSessionAccess", "userSessionVerification");

    private static final Map<String, String> FUNCTION_LABELS = Map.of(
            "userSessionCreatedByPin", "Innlogging med engangskode (SMS)",
            "userSessionCreatedByPassword", "Innlogging med passord",
            "userSessionRenewal", "Fornyelse av innlogget sesjon",
            "userSessionRemoved", "Utlogging / sesjon avsluttet",
            "userCreated", "Brukerkonto opprettet",
            "userSessionAccess", "Bruk av innlogget sesjon",
            "userSessionVerification", "Kontroll av innlogget sesjon");

    public record Session(long time, String function, String applicationId) {
    }

    /**
     * The result of reading one source: its JSON, or a description of why it could not be read.
     */
    public record Source(String json, String error) {
        public static Source ok(String json) {
            return new Source(json, null);
        }

        public static Source failed(String error) {
            return new Source(null, error);
        }

        boolean failed() {
            return error != null;
        }
    }

    private final String uid;
    private final String username;
    private final Source crm;
    private final List<Source> sessionSources;
    private final Map<String, String> applicationNames;
    private final LocalDate from;
    private final LocalDate to;
    private final Instant generatedAt;

    public InnsynReport(String uid, String username, Source crm, List<Source> sessionSources,
                        Map<String, String> applicationNames, LocalDate from, LocalDate to, Instant generatedAt) {
        this.uid = uid;
        this.username = username;
        this.crm = crm;
        this.sessionSources = sessionSources;
        this.applicationNames = applicationNames != null ? applicationNames : Map.of();
        this.from = from;
        this.to = to;
        this.generatedAt = generatedAt;
    }

    /**
     * Session events from the statistics service, newest first. Each STS node reports the same event, so rows with
     * the same time, function and application are the same event and are kept once.
     */
    static List<Session> parseSessions(List<Source> sources) {
        Map<String, Session> unique = new LinkedHashMap<>();
        for (Source source : sources) {
            if (source.failed() || source.json() == null) {
                continue;
            }
            try {
                JsonNode rows = MAPPER.readTree(source.json()).path("activities").path("userSessions");
                for (JsonNode row : rows) {
                    long time = row.path("startTime").asLong(0);
                    JsonNode data = row.path("data");
                    String function = data.path("usersessionfunction").asText("");
                    String applicationId = data.path("applicationid").asText("");
                    if (time == 0) {
                        continue;
                    }
                    unique.putIfAbsent(time + "|" + function + "|" + applicationId, new Session(time, function, applicationId));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Unreadable session statistics", e);
            }
        }
        List<Session> sessions = new ArrayList<>(unique.values());
        sessions.sort(Comparator.comparingLong(Session::time).reversed());
        return sessions;
    }

    /**
     * Flattens the CRM customer JSON into label/value rows, so every stored field is shown whatever its shape.
     */
    static List<String[]> flatten(String json) {
        List<String[]> rows = new ArrayList<>();
        try {
            flatten("", MAPPER.readTree(json), rows);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable CRM customer", e);
        }
        return rows;
    }

    private static void flatten(String path, JsonNode node, List<String[]> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> flatten(path.isEmpty() ? e.getKey() : path + "." + e.getKey(), e.getValue(), rows));
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(path + "[" + (i + 1) + "]", node.get(i), rows);
            }
        } else if (!node.asText().isEmpty()) {
            rows.add(new String[]{path, node.asText()});
        }
    }

    public String render() {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"no\"><head><meta charset=\"utf-8\">")
                .append("<title>Innsyn i personopplysninger</title><style>")
                .append("body{font-family:Arial,Helvetica,sans-serif;color:#222;margin:32px;max-width:960px}")
                .append("h1{font-size:22px}h2{font-size:17px;margin-top:28px;border-bottom:1px solid #ccc;padding-bottom:4px}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px}th,td{text-align:left;padding:4px 8px;border-bottom:1px solid #eee;vertical-align:top}")
                .append("th{background:#f4f4f4}.meta td:first-child{width:220px;color:#555}.note{color:#555;font-size:13px}")
                .append(".error{color:#a00;font-weight:bold}@media print{body{margin:0}button{display:none}}")
                .append("</style></head><body>");
        html.append("<button onclick=\"window.print()\">Skriv ut / lagre som PDF</button>");
        html.append("<h1>Innsyn i personopplysninger</h1>");
        html.append("<table class=\"meta\">");
        row(html, "Gjelder bruker-ID", uid);
        row(html, "Brukernavn", username);
        row(html, "Periode for sesjonshistorikk", DAY.format(from.atStartOfDay(ZONE)) + " til " + DAY.format(to.atStartOfDay(ZONE)));
        row(html, "Rapport laget", TIME.format(generatedAt));
        html.append("</table>");

        renderCrm(html);
        renderSessions(html);

        html.append("<h2>Om opplysningene</h2><p class=\"note\">Sesjonshistorikk (innlogginger og bruk av innlogget sesjon) ")
                .append("lagres i en begrenset periode, normalt én måned, og slettes deretter automatisk. ")
                .append("Eldre hendelser finnes derfor ikke lenger og kan ikke utleveres. ")
                .append("Tidspunkter er oppgitt i norsk tid.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private void renderCrm(StringBuilder html) {
        html.append("<h2>Kundeopplysninger</h2>");
        if (crm == null) {
            html.append("<p>Det er ikke registrert en kundeprofil for denne brukeren.</p>");
            return;
        }
        if (crm.failed()) {
            html.append("<p class=\"error\">Kundeopplysningene kunne ikke hentes: ").append(esc(crm.error()))
                    .append(". Rapporten er ufullstendig, prøv igjen senere.</p>");
            return;
        }
        List<String[]> rows = flatten(crm.json());
        if (rows.isEmpty()) {
            html.append("<p>Kundeprofilen inneholder ingen utfylte opplysninger.</p>");
            return;
        }
        html.append("<table><tr><th>Felt</th><th>Verdi</th></tr>");
        for (String[] r : rows) {
            html.append("<tr><td>").append(esc(r[0])).append("</td><td>").append(esc(r[1])).append("</td></tr>");
        }
        html.append("</table>");
    }

    private void renderSessions(StringBuilder html) {
        html.append("<h2>Innlogginger og kontohendelser</h2>");
        boolean anyFailed = false;
        for (Source s : sessionSources) {
            if (s.failed()) {
                anyFailed = true;
                html.append("<p class=\"error\">Sesjonshistorikken kunne ikke hentes fullstendig: ").append(esc(s.error()))
                        .append(". Rapporten er ufullstendig, prøv igjen senere.</p>");
            }
        }
        List<Session> sessions = parseSessions(sessionSources);
        List<Session> events = sessions.stream().filter(s -> !ROUTINE.contains(s.function())).toList();
        if (events.isEmpty()) {
            html.append(anyFailed ? "" : "<p>Ingen innlogginger eller kontohendelser er registrert i perioden.</p>");
        } else {
            html.append("<table><tr><th>Tidspunkt</th><th>Hendelse</th><th>Tjeneste</th></tr>");
            for (Session s : events) {
                sessionRow(html, s);
            }
            html.append("</table>");
        }

        html.append("<h2>Bruk av innlogget sesjon per dag</h2>");
        Map<String, Integer> perDay = new TreeMap<>(Comparator.reverseOrder());
        for (Session s : sessions) {
            if (ROUTINE.contains(s.function())) {
                perDay.merge(DAY.format(Instant.ofEpochMilli(s.time())) + "|" + appName(s.applicationId()), 1, Integer::sum);
            }
        }
        if (perDay.isEmpty()) {
            html.append(anyFailed ? "" : "<p>Ingen bruk av innlogget sesjon er registrert i perioden.</p>");
        } else {
            html.append("<p class=\"note\">Når du er innlogget, kontrollerer tjenestene jevnlig at innloggingen fortsatt er gyldig. ")
                    .append("Hver kontroll registreres. Alle enkelthendelser står i vedlegget.</p>");
            html.append("<table><tr><th>Dato</th><th>Tjeneste</th><th>Antall</th></tr>");
            perDay.forEach((key, count) -> {
                String[] p = key.split("\\|", 2);
                html.append("<tr><td>").append(esc(p[0])).append("</td><td>").append(esc(p[1])).append("</td><td>").append(count).append("</td></tr>");
            });
            html.append("</table>");
        }

        if (!sessions.isEmpty()) {
            html.append("<h2>Vedlegg: alle registrerte sesjonshendelser</h2>");
            html.append("<table><tr><th>Tidspunkt</th><th>Hendelse</th><th>Tjeneste</th></tr>");
            for (Session s : sessions) {
                sessionRow(html, s);
            }
            html.append("</table>");
        }
    }

    private void sessionRow(StringBuilder html, Session s) {
        html.append("<tr><td>").append(TIME.format(Instant.ofEpochMilli(s.time()))).append("</td><td>")
                .append(esc(FUNCTION_LABELS.getOrDefault(s.function(), s.function()))).append("</td><td>")
                .append(esc(appName(s.applicationId()))).append("</td></tr>");
    }

    private String appName(String applicationId) {
        String name = applicationNames.get(applicationId);
        return name != null && !name.isBlank() ? name + " (" + applicationId + ")" : applicationId;
    }

    private static void row(StringBuilder html, String label, String value) {
        html.append("<tr><td>").append(esc(label)).append("</td><td>").append(esc(value)).append("</td></tr>");
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
