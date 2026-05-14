package depth.finvibe.modules.asset.infra.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.ValuationWriteBackService;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ValuationWriteBackScheduler {
    private final ValuationWriteBackService valuationWriteBackService;

    public void executeWriteBack() {
        try {
            valuationWriteBackService.runWriteBackBatch();
        } catch (Exception ex) {
            log.error("Failed to execute valuation write-back batch", ex);
            throw ex;
        }
    }
}
