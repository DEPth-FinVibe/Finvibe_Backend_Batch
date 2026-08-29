package depth.finvibe.modules.market.infra.scheduler;

import depth.finvibe.modules.market.application.RankingCandleWarmUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RankingCandleWarmUpScheduler {

    private final RankingCandleWarmUpService rankingCandleWarmUpService;

    public void warmUpHomeRankingCandles() {
        try {
            rankingCandleWarmUpService.warmUpHomeRankingCandles();
        } catch (Exception exception) {
            // 워밍업 실패가 다른 배치를 막지 않도록 여기서 흡수한다. 조회는 기존 폴백 경로로 동작한다.
            log.error("홈 랭킹 캔들 워밍업에 실패했습니다.", exception);
        }
    }
}
