package depth.finvibe.modules.asset.infra.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValuationRedisExclusiveLock {
    private static final String LOCK_NAME = "valuationRedisExclusive";

    private final LockProvider lockProvider;

    public boolean runWithLock(String operation, Duration lockAtMostFor, Duration lockAtLeastFor, Runnable action) {
        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
            Instant.now(),
            LOCK_NAME,
            lockAtMostFor,
            lockAtLeastFor
        ));

        if (lock.isEmpty()) {
            log.info("Skip {} because valuation Redis exclusive lock is already held. lockName={}", operation, LOCK_NAME);
            return false;
        }

        try {
            log.info("Acquired valuation Redis exclusive lock. operation={}, lockName={}", operation, LOCK_NAME);
            action.run();
            return true;
        } finally {
            lock.get().unlock();
            log.info("Released valuation Redis exclusive lock. operation={}, lockName={}", operation, LOCK_NAME);
        }
    }
}
