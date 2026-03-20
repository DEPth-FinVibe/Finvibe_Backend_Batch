package depth.finvibe.batch.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.batch.config.ScheduledBatchJobSupport;

@Component
@RequiredArgsConstructor
public class ScheduledBatchLauncher {

	private final ScheduledBatchJobSupport scheduledBatchJobSupport;

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
	@Qualifier("syncRealtimeSubscriptionsJob")
	private final Job syncRealtimeSubscriptionsJob;
	@Qualifier("ensureNextMonthHolidayCalendarJob")
	private final Job ensureNextMonthHolidayCalendarJob;
	@Qualifier("recoverStaleCurrentPricesJob")
	private final Job recoverStaleCurrentPricesJob;
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

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_updateWeeklySquadRanking", lockAtMostFor = "PT30M")
	public void updateWeeklySquadRanking() {
		scheduledBatchJobSupport.launch(updateWeeklySquadRankingJob);
	}

	@Scheduled(cron = "0 55 23 * * SUN", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_rewardPersonalChallenges", lockAtMostFor = "PT30M")
	public void rewardPersonalChallenges() {
		scheduledBatchJobSupport.launch(rewardPersonalChallengesJob);
	}

	@Scheduled(cron = "0 58 23 * * SUN", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_rewardWeeklyChallenges", lockAtMostFor = "PT30M")
	public void rewardWeeklyChallenges() {
		scheduledBatchJobSupport.launch(rewardWeeklyChallengesJob);
	}

	@Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_generatePersonalChallenges", lockAtMostFor = "PT30M")
	public void generatePersonalChallenges() {
		scheduledBatchJobSupport.launch(generatePersonalChallengesJob);
	}

	@Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "gamification_refreshUserRankingSnapshots", lockAtMostFor = "PT30M")
	public void refreshUserRankingSnapshots() {
		scheduledBatchJobSupport.launch(refreshUserRankingSnapshotsJob);
	}

	@Scheduled(cron = "${news.crawler.cron:0 0 0 * * *}", zone = "Asia/Seoul")
	@SchedulerLock(name = "NewsModuleScheduler.syncLatestNews", lockAtLeastFor = "PT1M", lockAtMostFor = "PT2H")
	public void syncLatestNews() {
		scheduledBatchJobSupport.launch(syncLatestNewsJob);
	}

	@Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "NewsModuleScheduler.syncDiscussionCounts", lockAtLeastFor = "PT30S", lockAtMostFor = "PT30M")
	public void syncDiscussionCounts() {
		scheduledBatchJobSupport.launch(syncDiscussionCountsJob);
	}

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "closingPriceCleanup", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void cleanupClosingPriceOnMarketOpen() {
		scheduledBatchJobSupport.launch(cleanupClosingPriceOnMarketOpenJob);
	}

	@Scheduled(cron = "0 0 * * * *")
	@SchedulerLock(name = "batchPriceUpdate", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
	public void executeBatchPriceUpdate() {
		scheduledBatchJobSupport.launch(executeBatchPriceUpdateJob);
	}

	@Scheduled(cron = "${market.index-cache.cron:0 * * * * *}")
	@SchedulerLock(name = "indexMinuteCandleCache", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void cacheIndexMinuteCandles() {
		scheduledBatchJobSupport.launch(cacheIndexMinuteCandlesJob);
	}

	@Scheduled(fixedDelayString = "${market.kis.websocket.sync-interval-ms:1000}")
	public void syncRealtimeSubscriptions() {
		scheduledBatchJobSupport.launch(syncRealtimeSubscriptionsJob);
	}

	@Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "holidayCalendarScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
	public void ensureNextMonthHolidayCalendar() {
		scheduledBatchJobSupport.launch(ensureNextMonthHolidayCalendarJob);
	}

	@Scheduled(fixedDelayString = "${market.current-price.stale-recovery.interval-ms:3000}")
	@SchedulerLock(name = "staleCurrentPriceRecovery", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
	public void recoverStaleCurrentPrices() {
		scheduledBatchJobSupport.launch(recoverStaleCurrentPricesJob);
	}

	@Scheduled(cron = "0 */10 * * * *")
	@SchedulerLock(name = "stockRankingUpdate", lockAtMostFor = "PT1M", lockAtLeastFor = "PT5S")
	public void executeStockRankingUpdate() {
		scheduledBatchJobSupport.launch(executeStockRankingUpdateJob);
	}

	@Scheduled(cron = "0 0 2 * * *")
	@SchedulerLock(name = "stockBulkUpsert", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
	public void executeStockBulkUpsert() {
		scheduledBatchJobSupport.launch(executeStockBulkUpsertJob);
	}

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	@SchedulerLock(name = "userProfitRankingWeeklyBadge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void rewardWeeklyTopOnePercentBadge() {
		scheduledBatchJobSupport.launch(rewardWeeklyTopOnePercentBadgeJob);
	}

	@Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "userProfitRankingMonthlyBadge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void rewardMonthlyTopOnePercentBadge() {
		scheduledBatchJobSupport.launch(rewardMonthlyTopOnePercentBadgeJob);
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "userProfitSnapshotDaily", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void saveUserProfitDailySnapshot() {
		scheduledBatchJobSupport.launch(saveUserProfitDailySnapshotJob);
	}

	@Scheduled(cron = "0 7 0 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "portfolioPerformanceSnapshotDaily", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
	public void savePortfolioPerformanceDailySnapshot() {
		scheduledBatchJobSupport.launch(savePortfolioPerformanceDailySnapshotJob);
	}
}
