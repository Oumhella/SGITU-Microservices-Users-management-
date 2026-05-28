package ma.sgitu.g8;

import ma.sgitu.g8.ingestion.IngestionService;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.ReportRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import ma.sgitu.g8.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RestTemplate mockedRestTemplate;

    @Autowired
    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private EventRepository eventRepository;

    @Mock
    private StatSnapshotRepository statSnapshotRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private IngestionService ingestionService;

    @Test
    @DisplayName("IngestionService maps a valid ticket event into a persisted incoming event")
    void ingestionMapsAndPersistsEvent() {
        BatchIngestionResponse response = ingestionService.ingest(List.of(Map.of(
                "timestamp", "2026-05-05T11:00:00Z",
                "userId", "user-1",
                "status", "validated",
                "line", "L1"
        )), SourceType.TICKETING);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getTotalAccepted()).isEqualTo(1);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("AnalyticsService generates a report from non-prediction snapshots only")
    void analyticsServiceBuildsReportFromSnapshots() {
        AnalyticsService analyticsService = new AnalyticsService(statSnapshotRepository, reportRepository);

        // 10 ticket events
        List<Map<String, Object>> tickets = new ArrayList<>();
        int[] hours = {6, 8, 9, 12, 17, 18, 6, 8, 17, 18};
        String[] lines = {"L1", "L2", "L3", "L1", "L2", "L3", "L1", "L2", "L3", "L1"};
        for (int i = 0; i < 10; i++) {
            tickets.add(Map.of(
                    "schemaVersion", 1,
                    "timestamp", createTimestamp(hours[i]),
                    "userId", UUID.randomUUID().toString(),
                    "status", "validated",
                    "line", lines[i],
                    "stationId", "ST-" + (i % 3)
            ));
        }
        ResponseEntity<Map> ticketResp = restTemplate.postForEntity(
                "/api/v1/ingestion/tickets", tickets, Map.class);
        assertThat(ticketResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5 payment events
        List<Map<String, Object>> payments = new ArrayList<>();
        double[] amounts = {10.0, 20.0, 5.0, 50.0, 15.0};
        String[] methods = {"CARD", "CASH", "CARD", "MOBILE", "CASH"};
        for (int i = 0; i < 5; i++) {
            payments.add(Map.of(
                    "schemaVersion", 1,
                    "timestamp", createTimestamp(10),
                    "transactionId", UUID.randomUUID().toString(),
                    "status", "completed",
                    "amount", amounts[i],
                    "paymentMethod", methods[i],
                    "paymentType", "TICKET"
            ));
        }
        ResponseEntity<Map> paymentResp = restTemplate.postForEntity(
                "/api/v1/ingestion/payments", payments, Map.class);
        assertThat(paymentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5 incident events
        List<Map<String, Object>> incidents = new ArrayList<>();
        double[][] coordinates = {
                {33.5731, -7.5898},
                {33.5800, -7.6000},
                {33.5900, -7.6100},
                {33.5731, -7.5898},
                {33.5800, -7.6000}
        };
        String[] severities = {"LOW", "MEDIUM", "HIGH", "CRITICAL", "LOW"};
        for (int i = 0; i < 5; i++) {
            incidents.add(Map.of(
                    "schemaVersion", 1,
                    "timestamp", createTimestamp(10),
                    "incidentId", UUID.randomUUID().toString(),
                    "type", "delay",
                    "latitude", coordinates[i][0],
                    "longitude", coordinates[i][1],
                    "severity", severities[i]
            ));
        }
        ResponseEntity<Map> incidentResp = restTemplate.postForEntity(
                "/api/v1/ingestion/incidents", incidents, Map.class);
        assertThat(incidentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(report.getPeriod()).isEqualTo("2026-05-05");
        assertThat(report.getSnapshots()).containsExactly(actualSnapshot);
    }

    @Test
    @DisplayName("AnalyticsService returns a report when the repository finds one")
    void analyticsServiceReturnsReportById() {
        AnalyticsService analyticsService = new AnalyticsService(statSnapshotRepository, reportRepository);
        Report report = Report.builder().id("report-1").period("2026-05-05").build();

        when(reportRepository.findById(anyString())).thenReturn(Optional.of(report));

        // =====================================================================
        // Step 3 - Verify snapshots were created
        // =====================================================================
        List<StatSnapshot> snapshots = snapshotRepository.findAll();
        assertThat(snapshots.size()).isGreaterThanOrEqualTo(10);

        boolean hasFreq = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("FREQ_"));
        boolean hasRev = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("REV_"));
        boolean hasInc = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("INC_"));

        assertThat(hasFreq).as("Should have FREQ_ snapshots").isTrue();
        assertThat(hasRev).as("Should have REV_ snapshots").isTrue();
        assertThat(hasInc).as("Should have INC_ snapshots").isTrue();

        snapshots.forEach(s -> assertThat(s.getSchemaVersion())
                .as("Snapshot %s must carry schemaVersion=1", s.getStatId())
                .isEqualTo(1));


        // =====================================================================
        // Step 4 - Verify analytics endpoints return data
        // =====================================================================
        ResponseEntity<List> dashboardResp = restTemplate.getForEntity(
                "/api/v1/analytics/dashboard", List.class);
        assertThat(dashboardResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboardResp.getBody()).isNotEmpty();


        // =====================================================================
        // Step 5 - Verify report generation works
        // =====================================================================
        String reportPeriod = LocalDateTime.now().toLocalDate().toString();
        Map<String, Object> reportReq = Map.of(
                "period", reportPeriod,
                "types", List.of("TRIPS", "REVENUE", "INCIDENTS")
        );

        ResponseEntity<Report> generateResp = restTemplate.postForEntity(
                "/api/v1/analytics/reports/generate", reportReq, Report.class);
        assertThat(generateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Report generatedReport = generateResp.getBody();
        assertThat(generatedReport).isNotNull();
        assertThat(generatedReport.getId()).isNotNull();

        // Get the generated report
        ResponseEntity<Report> getReportResp = restTemplate.getForEntity(
                "/api/v1/analytics/reports/" + generatedReport.getId(), Report.class);
        assertThat(getReportResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getReportResp.getBody()).isNotNull();
        assertThat(getReportResp.getBody().getId()).isEqualTo(generatedReport.getId());
        assertThat(getReportResp.getBody().getPeriod()).isEqualTo(reportPeriod);
    }
}
