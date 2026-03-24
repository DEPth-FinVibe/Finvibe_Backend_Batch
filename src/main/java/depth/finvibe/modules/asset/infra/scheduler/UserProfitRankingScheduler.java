package depth.finvibe.modules.asset.infra.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.UserProfitRankingBadgeService;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UserProfitRankingScheduler {
  private final UserProfitRankingBadgeService userProfitRankingBadgeService;

  public void rewardWeeklyTopOnePercentBadge() {
    userProfitRankingBadgeService.rewardWeeklyTopOnePercentBadge();
  }

  public void rewardMonthlyTopOnePercentBadge() {
    userProfitRankingBadgeService.rewardMonthlyTopOnePercentBadge();
  }
}
