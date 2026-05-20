package depth.finvibe.modules.asset.infra.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ValuationWarmUpStartupState {
    private final AtomicBoolean completed = new AtomicBoolean(false);

    @Value("${batch.startup-warm-up.enabled:true}")
    private boolean enabled;

    public boolean isCompleted() {
        return !enabled || completed.get();
    }

    public void markCompleted() {
        completed.set(true);
    }
}
