package depth.finvibe.batch.config;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import depth.finvibe.modules.asset.infra.scheduler.PortfolioPerformanceSnapshotScheduler;
import depth.finvibe.modules.asset.infra.scheduler.UserProfitRankingScheduler;
import depth.finvibe.modules.asset.infra.scheduler.UserProfitSnapshotScheduler;
import depth.finvibe.modules.gamification.infra.scheduler.GamificationScheduler;
import depth.finvibe.modules.market.infra.scheduler.BatchPriceUpdateScheduler;
import depth.finvibe.modules.market.infra.scheduler.ClosingPriceCleanupScheduler;
import depth.finvibe.modules.market.infra.scheduler.HolidayCalendarScheduler;
import depth.finvibe.modules.market.infra.scheduler.IndexMinuteCandleCacheScheduler;
import depth.finvibe.modules.market.infra.scheduler.StockBulkUpsertScheduler;
import depth.finvibe.modules.market.infra.scheduler.StockRankingUpdateScheduler;
import depth.finvibe.modules.news.infra.scheduler.NewsModuleScheduler;

@Configuration
public class TaskletBatchJobConfig {

	@Bean
	Job updateWeeklySquadRankingJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		GamificationScheduler gamificationScheduler
	) {
		return taskletJob(
			"updateWeeklySquadRankingJob",
			jobRepository,
			transactionManager,
			gamificationScheduler::updateWeeklySquadRanking
		);
	}

	@Bean
	Job rewardPersonalChallengesJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		GamificationScheduler gamificationScheduler
	) {
		return taskletJob(
			"rewardPersonalChallengesJob",
			jobRepository,
			transactionManager,
			gamificationScheduler::rewardPersonalChallenges
		);
	}

	@Bean
	Job rewardWeeklyChallengesJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		GamificationScheduler gamificationScheduler
	) {
		return taskletJob(
			"rewardWeeklyChallengesJob",
			jobRepository,
			transactionManager,
			gamificationScheduler::rewardWeeklyChallenges
		);
	}

	@Bean
	Job generatePersonalChallengesJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		GamificationScheduler gamificationScheduler
	) {
		return taskletJob(
			"generatePersonalChallengesJob",
			jobRepository,
			transactionManager,
			gamificationScheduler::generatePersonalChallenges
		);
	}

	@Bean
	Job refreshUserRankingSnapshotsJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		GamificationScheduler gamificationScheduler
	) {
		return taskletJob(
			"refreshUserRankingSnapshotsJob",
			jobRepository,
			transactionManager,
			gamificationScheduler::refreshUserRankingSnapshots
		);
	}

	@Bean
	Job syncLatestNewsJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		NewsModuleScheduler newsModuleScheduler
	) {
		return taskletJob(
			"syncLatestNewsJob",
			jobRepository,
			transactionManager,
			newsModuleScheduler::syncLatestNews
		);
	}

	@Bean
	Job syncDiscussionCountsJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		NewsModuleScheduler newsModuleScheduler
	) {
		return taskletJob(
			"syncDiscussionCountsJob",
			jobRepository,
			transactionManager,
			newsModuleScheduler::syncDiscussionCounts
		);
	}

	@Bean
	Job cleanupClosingPriceOnMarketOpenJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ClosingPriceCleanupScheduler closingPriceCleanupScheduler
	) {
		return taskletJob(
			"cleanupClosingPriceOnMarketOpenJob",
			jobRepository,
			transactionManager,
			closingPriceCleanupScheduler::cleanupClosingPriceOnMarketOpen
		);
	}

	@Bean
	Job executeBatchPriceUpdateJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		BatchPriceUpdateScheduler batchPriceUpdateScheduler
	) {
		return taskletJob(
			"executeBatchPriceUpdateJob",
			jobRepository,
			transactionManager,
			batchPriceUpdateScheduler::executeBatchPriceUpdate
		);
	}

	@Bean
	Job cacheIndexMinuteCandlesJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		IndexMinuteCandleCacheScheduler indexMinuteCandleCacheScheduler
	) {
		return taskletJob(
			"cacheIndexMinuteCandlesJob",
			jobRepository,
			transactionManager,
			indexMinuteCandleCacheScheduler::cacheIndexMinuteCandles
		);
	}

	@Bean
	Job ensureNextMonthHolidayCalendarJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		HolidayCalendarScheduler holidayCalendarScheduler
	) {
		return taskletJob(
			"ensureNextMonthHolidayCalendarJob",
			jobRepository,
			transactionManager,
			holidayCalendarScheduler::ensureNextMonthHolidayCalendar
		);
	}

	@Bean
	Job executeStockRankingUpdateJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		StockRankingUpdateScheduler stockRankingUpdateScheduler
	) {
		return taskletJob(
			"executeStockRankingUpdateJob",
			jobRepository,
			transactionManager,
			stockRankingUpdateScheduler::executeStockRankingUpdate
		);
	}

	@Bean
	Job executeStockBulkUpsertJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		StockBulkUpsertScheduler stockBulkUpsertScheduler
	) {
		return taskletJob(
			"executeStockBulkUpsertJob",
			jobRepository,
			transactionManager,
			stockBulkUpsertScheduler::executeStockBulkUpsert
		);
	}

	@Bean
	Job rewardWeeklyTopOnePercentBadgeJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		UserProfitRankingScheduler userProfitRankingScheduler
	) {
		return taskletJob(
			"rewardWeeklyTopOnePercentBadgeJob",
			jobRepository,
			transactionManager,
			userProfitRankingScheduler::rewardWeeklyTopOnePercentBadge
		);
	}

	@Bean
	Job rewardMonthlyTopOnePercentBadgeJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		UserProfitRankingScheduler userProfitRankingScheduler
	) {
		return taskletJob(
			"rewardMonthlyTopOnePercentBadgeJob",
			jobRepository,
			transactionManager,
			userProfitRankingScheduler::rewardMonthlyTopOnePercentBadge
		);
	}

	@Bean
	Job saveUserProfitDailySnapshotJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		UserProfitSnapshotScheduler userProfitSnapshotScheduler
	) {
		return taskletJob(
			"saveUserProfitDailySnapshotJob",
			jobRepository,
			transactionManager,
			userProfitSnapshotScheduler::saveDailySnapshot
		);
	}

	@Bean
	Job savePortfolioPerformanceDailySnapshotJob(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		PortfolioPerformanceSnapshotScheduler portfolioPerformanceSnapshotScheduler
	) {
		return taskletJob(
			"savePortfolioPerformanceDailySnapshotJob",
			jobRepository,
			transactionManager,
			portfolioPerformanceSnapshotScheduler::saveDailySnapshot
		);
	}

	private Job taskletJob(
		String jobName,
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		Runnable action
	) {
		Step step = new StepBuilder(jobName + "Step", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				action.run();
				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();

		return new JobBuilder(jobName, jobRepository)
			.start(step)
			.build();
	}
}
