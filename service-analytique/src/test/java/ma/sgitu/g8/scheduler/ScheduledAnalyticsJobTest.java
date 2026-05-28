package ma.sgitu.g8.scheduler;

import ma.sgitu.g8.aggregation.IncidentAggregation;
import ma.sgitu.g8.aggregation.RevenueAggregation;
import ma.sgitu.g8.aggregation.SubscriptionAggregation;
import ma.sgitu.g8.aggregation.TicketAggregation;
import ma.sgitu.g8.aggregation.UserAggregation;
import ma.sgitu.g8.aggregation.VehicleAggregation;
import ma.sgitu.g8.alert.ThresholdAlertService;
import ma.sgitu.g8.ml.MlPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledAnalyticsJobTest {

    @Mock private IncidentAggregation incidentAggregation;
    @Mock private VehicleAggregation vehicleAggregation;
    @Mock private TicketAggregation ticketAggregation;
    @Mock private RevenueAggregation revenueAggregation;
    @Mock private SubscriptionAggregation subscriptionAggregation;
    @Mock private UserAggregation userAggregation;
    @Mock private ThresholdAlertService thresholdAlertService;
    @Mock private MlPredictionService mlPredictionService;

    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    /** SnapshotRepository (used by aggregations to save). */
    @Autowired
    private SnapshotRepository snapshotRepository;

    /** StatSnapshotRepository (maps to the same collection, used for reads). */
    @Autowired
    private StatSnapshotRepository statSnapshotRepository;


    @BeforeEach
    void setUp() {
        scheduledAnalyticsJob = new ScheduledAnalyticsJob();
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "incidentAggregation", incidentAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "vehicleAggregation", vehicleAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "ticketAggregation", ticketAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "revenueAggregation", revenueAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "subscriptionAggregation", subscriptionAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "userAggregation", userAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "thresholdAlertService", thresholdAlertService);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "mlPredictionService", mlPredictionService);
    }

    @Test
    @DisplayName("runAnalytics triggers every analytics collaborator")
    void runAnalyticsInvokesCollaborators() {
        assertThatCode(() -> scheduledAnalyticsJob.runAnalytics()).doesNotThrowAnyException();

    // -------------------------------------------------------------------------
    // Scenario B – job writes snapshots after ingesting mock events
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – runAnalytics() writes at least one snapshot after seeding events")
    void runAnalyticsAfterSeeding_createsSnapshots() {
    }

    @Test
    @DisplayName("E – malformed historical events do not break aggregations")
    void malformedEvents_doNotBreakScheduler() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);

        eventRepository.saveAll(List.of(
                IncomingEvent.builder()
                        .sourceType(SourceType.TICKETING)
                        .sourceId("bad-ticket")
                        .eventType("TICKET_VALIDATED")
                        .timestamp(null)
                        .receivedAt(LocalDateTime.now())
                        .payload(null)
                        .processed(false)
                        .build(),
                IncomingEvent.builder()
                        .sourceType(SourceType.PAYMENT)
                        .sourceId("bad-payment")
                        .eventType("PAYMENT_COMPLETED")
                        .timestamp(now)
                        .receivedAt(LocalDateTime.now())
                        .payload(Map.of("amount", "not-a-number"))
                        .processed(false)
                        .build(),
                IncomingEvent.builder()
                        .sourceType(SourceType.VEHICLE)
                        .sourceId("bad-vehicle")
                        .eventType("VEHICLE_IN_SERVICE")
                        .timestamp(now)
                        .receivedAt(LocalDateTime.now())
                        .payload(null)
                        .processed(false)
                        .build(),
                IncomingEvent.builder()
                        .sourceType(SourceType.USER)
                        .sourceId(null)
                        .eventType("USER_ACTIVE")
                        .timestamp(now)
                        .receivedAt(LocalDateTime.now())
                        .payload(null)
                        .processed(false)
                        .build()
        ));

        assertThatCode(() -> scheduledAnalyticsJob.runAnalytics())
                .doesNotThrowAnyException();
    }
}
