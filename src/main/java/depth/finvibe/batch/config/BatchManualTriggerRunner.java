package depth.finvibe.batch.config;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import depth.finvibe.modules.asset.infra.scheduler.PortfolioPerformanceSnapshotScheduler;
import depth.finvibe.modules.asset.infra.scheduler.UserProfitSnapshotScheduler;
import depth.finvibe.modules.asset.infra.scheduler.ValuationWarmUpScheduler;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "batch.manual-trigger", name = "enabled", havingValue = "true")
public class BatchManualTriggerRunner implements ApplicationRunner {

	private final UserProfitSnapshotScheduler userProfitSnapshotScheduler;
	private final PortfolioPerformanceSnapshotScheduler portfolioPerformanceSnapshotScheduler;
	private final ValuationWarmUpScheduler valuationWarmUpScheduler;

	@Value("${batch.manual-trigger.jobs:}")
	private String manualTriggerJobs;

	@Value("${batch.manual-trigger.snapshot-date:}")
	private String snapshotDateValue;

	@Override
	public void run(ApplicationArguments args) {
		LocalDate snapshotDate = parseSnapshotDate();

		Arrays.stream(manualTriggerJobs.split(","))
				.map(String::trim)
				.filter(job -> !job.isEmpty())
				.map(job -> job.toLowerCase(Locale.ROOT))
				.forEach(job -> trigger(job, snapshotDate));
	}

	private void trigger(String job, LocalDate snapshotDate) {
			switch (job) {
			case "valuation-warm-up" -> {
				log.info("Manually triggering valuation warm-up");
				valuationWarmUpScheduler.executeWarmUp();
			}
			case "user-profit-snapshot" -> {
				log.info("Manually triggering user profit snapshot for date {}", snapshotDate);
				userProfitSnapshotScheduler.saveSnapshot(snapshotDate);
			}
			case "portfolio-performance-snapshot" -> {
				log.info("Manually triggering portfolio performance snapshot for date {}", snapshotDate);
				portfolioPerformanceSnapshotScheduler.saveSnapshot(snapshotDate);
			}
			case "all" -> {
				log.info("Manually triggering all manual batch jobs for date {}", snapshotDate);
				valuationWarmUpScheduler.executeWarmUp();
				userProfitSnapshotScheduler.saveSnapshot(snapshotDate);
				portfolioPerformanceSnapshotScheduler.saveSnapshot(snapshotDate);
			}
			default -> throw new IllegalArgumentException("Unknown batch.manual-trigger.jobs entry: " + job);
		}
	}

	private LocalDate parseSnapshotDate() {
		if (snapshotDateValue == null || snapshotDateValue.isBlank()) {
			return LocalDate.now();
		}

		return LocalDate.parse(snapshotDateValue.trim());
	}
}
