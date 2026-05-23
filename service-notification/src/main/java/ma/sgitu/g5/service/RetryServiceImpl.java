package ma.sgitu.g5.service;

import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RetryServiceImpl implements IRetryService {

    private static final int MAX_RETRIES = 3;

    @Override
    public boolean shouldRetry(int currentRetryCount) {
        return currentRetryCount < MAX_RETRIES;
    }

    @Override
    public int nextDelaySeconds(int currentRetryCount) {
        // Backoff exponentiel : 30s, 60s, 120s
        return (int) (30 * Math.pow(2, currentRetryCount));
    }

    @Override
    public void scheduleRetry(String notificationId, int delaySeconds) {
        log.warn("[G5-RETRY] Retry planifié pour notificationId={} dans {}s",
                notificationId, delaySeconds);
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                log.info("[G5-RETRY] Retry exécuté pour notificationId={}", notificationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[G5-RETRY] Retry interrompu pour notificationId={}", notificationId);
            }
        });
    }
}
