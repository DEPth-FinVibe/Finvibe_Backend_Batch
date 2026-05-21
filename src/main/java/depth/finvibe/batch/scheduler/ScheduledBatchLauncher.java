package depth.finvibe.batch.scheduler;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.batch.config.ScheduledBatchJobSupport;
import depth.finvibe.modules.asset.infra.scheduler.ValuationRedisExclusiveLock;
import depth.finvibe.modules.asset.infra.scheduler.ValuationWarmUpStartupState;

@Component
@RequiredArgsConstructor
public class ScheduledBatchLauncher {

	private final ScheduledBatchJobSupport scheduledBatchJobSupport;
	private final ValuationRedisExclusiveLock valuationRedisExclusiveLock;
	private final ValuationWarmUpStartupState valuationWarmUpStartupState;

	@Qualifier("updateWeeklySquadRankingJob")
	private final Job updateWeeklySquadRankingJob;
	@Qualifier("rewardPersonalChallengesJob")
	private final Job rewardPersonalChallengesJob;
	@Qualifier("rewardWeeklyChallengesJob")
	private final Job rewardWeeklyChallengesJob;
	@Qualifier("generatePersonalChallengesJob")
	private final Job generatePersonalChallengesJob;
	@Qualifier("refreshUserRankingSnapshotsJob")
	private final Job refreshUserRankingSnapshotsJob;
	@Qualifier("syncLatestNewsJob")
	private final Job syncLatestNewsJob;
	@Qualifier("syncDiscussionCountsJob")
	private final Job syncDiscussionCountsJob;
	@Qualifier("cleanupClosingPriceOnMarketOpenJob")
	private final Job cleanupClosingPriceOnMarketOpenJob;
	@Qualifier("executeBatchPriceUpdateJob")
	private final Job executeBatchPriceUpdateJob;
	@Qualifier("cacheIndexMinuteCandlesJob")
	private final Job cacheIndexMinuteCandlesJob;
	@Qualifier("ensureNextMonthHolidayCalendarJob")
	private final Job ensureNextMonthHolidayCalendarJob;
	@Qualifier("executeStockRankingUpdateJob")
	private final Job executeStockRankingUpdateJob;
	@Qualifier("executeStockBulkUpsertJob")
	private final Job executeStockBulkUpsertJob;
	@Qualifier("rewardWeeklyTopOnePercentBadgeJob")
	private final Job rewardWeeklyTopOnePercentBadgeJob;
	@Qualifier("rewardMonthlyTopOnePercentBadgeJob")
	private final Job rewardMonthlyTopOnePercentBadgeJob;
	@Qualifier("saveUserProfitDailySnapshotJob")
	private final Job saveUserProfitDailySnapshotJob;
	@Qualifier("savePortfolioPerformanceDailySnapshotJob")
	private final Job savePortfolioPerformanceDailySnapshotJob;
	@Qualifier("executeValuationWarmUpJob")
	private final Job executeValuationWarmUpJob;
	@Qualifier("executeValuationWriteBackJob")
	private final Job executeValuationWriteBackJob;

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_updateWeeklySquadRanking", lockAtMostFor = "PT30M")
	public void updateWeeklySquadRanking() {
		launchAfterStartupWarmUp(updateWeeklySquadRankingJob);
	}

	@Scheduled(cron = "0 55 23 * * SUN", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_rewardPersonalChallenges", lockAtMostFor = "PT30M")
	public void rewardPersonalChallenges() {
		launchAfterStartupWarmUp(rewardPersonalChallengesJob);
	}

	@Scheduled(cron = "0 58 23 * * SUN", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_rewardWeeklyChallenges", lockAtMostFor = "PT30M")
	public void rewardWeeklyChallenges() {
		launchAfterStartupWarmUp(rewardWeeklyChallengesJob);
	}

	@Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_generatePersonalChallenges", lockAtMostFor = "PT30M")
	public void generatePersonalChallenges() {
		launchAfterStartupWarmUp(generatePersonalChallengesJob);
	}

	@Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_refreshUserRankingSnapshots", lockAtMostFor = "PT30M")
	public void refreshUserRankingSnapshots() {
		launchAfterStartupWarmUp(refreshUserRankingSnapshotsJob);
	}

	@Scheduled(cron = "${news.crawler.cron:0 0 0 * * *}", zone = "Asia/Seoul")
	@SchedulerLock(name = "NewsModuleScheduler.syncLatestNews", lockAtLeastFor = "PT1M", lockAtMostFor = "PT2H")
	public void syncLatestNews() {
		launchAfterStartupWarmUp(syncLatestNewsJob);
	}

	@Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "NewsModuleScheduler.syncDiscussionCounts", lockAtLeastFor = "PT30S", lockAtMostFor = "PT30M")
	public void syncDiscussionCounts() {
		launchAfterStartupWarmUp(syncDiscussionCountsJob);
	}

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "closingPriceCleanup", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void cleanupClosingPriceOnMarketOpen() {
		launchAfterStartupWarmUp(cleanupClosingPriceOnMarketOpenJob);
	}

	@Scheduled(cron = "0 0 * * * *")
	@SchedulerLock(name = "batchPriceUpdate", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
	public void executeBatchPriceUpdate() {
		launchAfterStartupWarmUp(executeBatchPriceUpdateJob);
	}

	@Scheduled(cron = "${market.index-cache.cron:0 * * * * *}")
	@SchedulerLock(name = "indexMinuteCandleCache", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void cacheIndexMinuteCandles() {
		launchAfterStartupWarmUp(cacheIndexMinuteCandlesJob);
	}

	@Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "holidayCalendarScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
	public void ensureNextMonthHolidayCalendar() {
		launchAfterStartupWarmUp(ensureNextMonthHolidayCalendarJob);
	}

	@Scheduled(cron = "0 */10 * * * *")
	@SchedulerLock(name = "stockRankingUpdate", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void executeStockRankingUpdate() {
		launchAfterStartupWarmUp(executeStockRankingUpdateJob);
	}

	@Scheduled(cron = "0 0 2 * * *")
	@SchedulerLock(name = "stockBulkUpsert", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
	public void executeStockBulkUpsert() {
		launchAfterStartupWarmUp(executeStockBulkUpsertJob);
	}

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "userProfitRankingWeeklyBadge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void rewardWeeklyTopOnePercentBadge() {
		launchAfterStartupWarmUp(rewardWeeklyTopOnePercentBadgeJob);
	}

	@Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "userProfitRankingMonthlyBadge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void rewardMonthlyTopOnePercentBadge() {
		launchAfterStartupWarmUp(rewardMonthlyTopOnePercentBadgeJob);
	}

	@Scheduled(cron = "${batch.schedule.user-profit-snapshot.cron:0 5 * * * *}", zone = "${batch.schedule.zone:Asia/Seoul}")
	@SchedulerLock(name = "userProfitSnapshotDaily", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void saveUserProfitDailySnapshot() {
		launchAfterStartupWarmUp(saveUserProfitDailySnapshotJob);
	}

	@Scheduled(cron = "${batch.schedule.portfolio-performance-snapshot.cron:0 7 * * * *}", zone = "${batch.schedule.zone:Asia/Seoul}")
	@SchedulerLock(name = "portfolioPerformanceSnapshotDaily", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void savePortfolioPerformanceDailySnapshot() {
		launchAfterStartupWarmUp(savePortfolioPerformanceDailySnapshotJob);
	}

	@Scheduled(cron = "${batch.schedule.valuation-warm-up.cron:0 30 0 * * *}", zone = "${batch.schedule.zone:Asia/Seoul}")
	public void executeValuationWarmUp() {
		if (shouldSkipUntilStartupWarmUpCompleted()) {
			return;
		}

		valuationRedisExclusiveLock.runWithLock(
				"scheduled valuation warm-up",
				Duration.ofHours(2),
				Duration.ofMinutes(1),
				() -> scheduledBatchJobSupport.launch(executeValuationWarmUpJob)
		);
	}

	@Scheduled(cron = "${batch.schedule.valuation-write-back.cron:0/30 * * * * *}", zone = "${batch.schedule.zone:Asia/Seoul}")
	public void executeValuationWriteBack() {
		if (shouldSkipUntilStartupWarmUpCompleted()) {
			return;
		}

		valuationRedisExclusiveLock.runWithLock(
				"scheduled valuation write-back",
				Duration.ofMinutes(5),
				Duration.ofSeconds(1),
				() -> scheduledBatchJobSupport.launch(executeValuationWriteBackJob)
		);
	}

	private void launchAfterStartupWarmUp(Job job) {
		if (shouldSkipUntilStartupWarmUpCompleted()) {
			return;
		}

		scheduledBatchJobSupport.launch(job);
	}

	private boolean shouldSkipUntilStartupWarmUpCompleted() {
		return !valuationWarmUpStartupState.isCompleted();
	}
}
