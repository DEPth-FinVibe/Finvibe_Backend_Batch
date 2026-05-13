package depth.finvibe.modules.asset.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.port.out.UserProfitSnapshotRepository;
import depth.finvibe.modules.asset.application.port.out.UserValuationRepository;
import depth.finvibe.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.modules.asset.domain.UserProfitSnapshotDailyId;
import depth.finvibe.modules.asset.domain.UserValuation;
import depth.finvibe.common.investment.application.port.out.GamificationEventProducer;
import depth.finvibe.common.investment.dto.Badge;
import depth.finvibe.common.investment.dto.RewardBadgeEvent;
import depth.finvibe.modules.user.application.service.UserIdResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfitSnapshotService {
  private final UserProfitSnapshotRepository userProfitSnapshotRepository;
  private final UserValuationRepository userValuationRepository;
  private final GamificationEventProducer gamificationEventProducer;
  private final UserIdResolver userIdResolver;

  @Value("${batch.chunk.user-profit-snapshot-size:500}")
  private int userValuationChunkSize;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void saveDailySnapshot(LocalDate snapshotDate) {
    if (snapshotDate == null) {
      return;
    }

    String lastUserId = null;
    while (true) {
      List<UserValuation> valuations = userValuationRepository.findAfter(lastUserId, userValuationChunkSize);
      if (valuations.isEmpty()) {
        return;
      }

      processValuationChunk(valuations, snapshotDate);

      lastUserId = valuations.get(valuations.size() - 1).getUserId();
      userProfitSnapshotRepository.flushAndClear();
    }
  }

  private void processValuationChunk(List<UserValuation> valuations, LocalDate snapshotDate) {
    List<Long> internalUserIds = valuations.stream()
      .map(UserValuation::getUserId)
      .map(this::resolveInternalUserId)
      .flatMap(java.util.Optional::stream)
      .toList();

    Set<Long> usersWithPositiveProfitHistory = userProfitSnapshotRepository.findUserIdsWithPositiveProfitSnapshot(
      internalUserIds,
      BigDecimal.ZERO,
      snapshotDate
    );

    List<UserProfitSnapshotDaily> snapshots = new ArrayList<>();
    for (UserValuation valuation : valuations) {
      Long internalUserId = resolveInternalUserId(valuation.getUserId()).orElse(null);
      if (internalUserId == null || valuation.getPortfolioCount() == null || valuation.getPortfolioCount() <= 0) {
        continue;
      }

      BigDecimal totalCurrentValue = BigDecimal.valueOf(valuation.getCurrentValue());
      BigDecimal totalProfitLoss = totalCurrentValue.subtract(BigDecimal.valueOf(valuation.getPurchasedValue()));
      BigDecimal totalReturnRate = BigDecimal.valueOf(valuation.getProfitRate());
      UserProfitSnapshotDailyId id = new UserProfitSnapshotDailyId(internalUserId, snapshotDate);
      publishFirstProfitBadgeIfEligible(valuation.getUserId(), internalUserId, totalProfitLoss, usersWithPositiveProfitHistory);
      snapshots.add(UserProfitSnapshotDaily.create(
        id,
        totalCurrentValue,
        totalProfitLoss,
        totalReturnRate
      ));
    }

    userProfitSnapshotRepository.saveAll(snapshots);
  }

  private void publishFirstProfitBadgeIfEligible(
    String userId,
    Long internalUserId,
    BigDecimal totalProfitLoss,
    Set<Long> usersWithPositiveProfitHistory
  ) {
    if (userId == null || internalUserId == null || totalProfitLoss == null || usersWithPositiveProfitHistory == null) {
      return;
    }
    if (totalProfitLoss.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    if (usersWithPositiveProfitHistory.contains(internalUserId)) {
      return;
    }

    gamificationEventProducer.publishRewardBadgeEvent(RewardBadgeEvent.builder()
      .userId(userId)
      .badgeCode(Badge.FIRST_PROFIT.name())
      .issuedAt(Instant.now())
      .reason("첫 수익 달성")
      .build());
  }

  private java.util.Optional<Long> resolveInternalUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return java.util.Optional.empty();
    }

    try {
      return userIdResolver.resolveInternalUserIdIfPresent(UUID.fromString(userId));
    } catch (IllegalArgumentException ignored) {
      try {
        return java.util.Optional.of(Long.valueOf(userId));
      } catch (NumberFormatException ex) {
        log.warn("Skip user profit snapshot for unsupported userId format. userId={}", userId);
        return java.util.Optional.empty();
      }
    }
  }
}
