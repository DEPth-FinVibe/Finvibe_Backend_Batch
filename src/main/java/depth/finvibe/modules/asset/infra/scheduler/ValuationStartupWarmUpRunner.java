package depth.finvibe.modules.asset.infra.scheduler;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "batch.startup-warm-up", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ValuationStartupWarmUpRunner implements ApplicationRunner {
    private final ValuationWarmUpScheduler valuationWarmUpScheduler;
    private final ValuationRedisExclusiveLock valuationRedisExclusiveLock;
    private final ValuationWarmUpStartupState startupState;

    @Value("${batch.startup-warm-up.lock-wait-timeout:PT10M}")
    private Duration lockWaitTimeout;

    @Value("${batch.startup-warm-up.lock-retry-interval:PT5S}")
    private Duration lockRetryInterval;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Instant deadline = Instant.now().plus(lockWaitTimeout);
        log.info("Startup valuation warm-up started. lockWaitTimeout={}, lockRetryInterval={}",
                lockWaitTimeout, lockRetryInterval);

        while (Instant.now().isBefore(deadline)) {
            boolean executed = valuationRedisExclusiveLock.runWithLock(
                    "startup valuation warm-up",
                    Duration.ofHours(2),
                    Duration.ofMinutes(1),
                    valuationWarmUpScheduler::executeWarmUp
            );

            if (executed) {
                startupState.markCompleted();
                log.info("Startup valuation warm-up completed");
                return;
            }

            log.info("Startup valuation warm-up is waiting for valuation Redis exclusive lock");
            Thread.sleep(lockRetryInterval.toMillis());
        }

        throw new IllegalStateException("Startup valuation warm-up could not acquire valuation Redis exclusive lock within "
                + lockWaitTimeout);
    }
}
