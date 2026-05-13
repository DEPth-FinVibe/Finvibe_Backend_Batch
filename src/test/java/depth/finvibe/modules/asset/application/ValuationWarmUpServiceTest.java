package depth.finvibe.modules.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import depth.finvibe.modules.asset.application.dto.PortfolioWarmUpState;
import depth.finvibe.modules.asset.application.dto.UserWarmUpState;
import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationWarmUpRedisRepository;
import depth.finvibe.modules.asset.domain.Asset;
import depth.finvibe.modules.asset.domain.Currency;
import depth.finvibe.modules.asset.domain.Money;
import depth.finvibe.modules.asset.domain.PortfolioGroup;

@ExtendWith(MockitoExtension.class)
class ValuationWarmUpServiceTest {
    @Mock
    private PortfolioGroupRepository portfolioGroupRepository;

    @Mock
    private ValuationWarmUpRedisRepository valuationWarmUpRedisRepository;

    @Mock
    private EntityManager entityManager;

    private ValuationWarmUpService service;

    @BeforeEach
    void setUp() {
        service = new ValuationWarmUpService(
            portfolioGroupRepository,
            valuationWarmUpRedisRepository,
            entityManager,
            new SimpleMeterRegistry()
        );
        ReflectionTestUtils.setField(service, "chunkSize", 2);
    }

    @Test
    void warmsUpPortfolioAndUserStateInKeysetChunksWithoutCurrentPriceFields() {
        UUID userId = UUID.randomUUID();
        PortfolioGroup portfolio1 = PortfolioGroup.builder()
            .id(1L)
            .userId(userId)
            .assets(List.of(
                asset(10L, "2", "1500"),
                asset(11L, "0", "3000")
            ))
            .build();
        PortfolioGroup portfolio2 = PortfolioGroup.builder()
            .id(2L)
            .userId(userId)
            .assets(List.of(asset(12L, "3", "2500.4")))
            .build();

        when(portfolioGroupRepository.findPortfolioIdsAfter(null, 2)).thenReturn(List.of(1L, 2L));
        when(portfolioGroupRepository.findPortfolioIdsAfter(2L, 2)).thenReturn(List.of());
        when(portfolioGroupRepository.findAllWithAssetsByIds(List.of(1L, 2L))).thenReturn(List.of(portfolio1, portfolio2));
        when(portfolioGroupRepository.findUserIdsAfter(null, 2)).thenReturn(List.of(userId));
        when(portfolioGroupRepository.findUserIdsAfter(userId, 2)).thenReturn(List.of());
        when(portfolioGroupRepository.findAllWithAssetsByUserIds(List.of(userId))).thenReturn(List.of(portfolio1, portfolio2));

        service.warmUp();

        ArgumentCaptor<List<PortfolioWarmUpState>> portfolioStatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(valuationWarmUpRedisRepository).writePortfolioStates(portfolioStatesCaptor.capture());
        List<PortfolioWarmUpState> portfolioStates = portfolioStatesCaptor.getValue();
        assertThat(portfolioStates).hasSize(2);
        assertThat(portfolioStates.get(0).portfolioId()).isEqualTo(1L);
        assertThat(portfolioStates.get(0).purchasedValue()).isEqualTo(1500L);
        assertThat(portfolioStates.get(0).assetCount()).isEqualTo(1L);
        assertThat(portfolioStates.get(0).holdings()).extracting(PortfolioWarmUpState.Holding::stockId).containsExactly(10L);
        assertThat(portfolioStates.get(1).purchasedValue()).isEqualTo(2500L);
        assertThat(portfolioStates.get(1).assetCount()).isEqualTo(1L);

        ArgumentCaptor<List<UserWarmUpState>> userStatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(valuationWarmUpRedisRepository).writeUserStates(userStatesCaptor.capture());
        List<UserWarmUpState> userStates = userStatesCaptor.getValue();
        assertThat(userStates).hasSize(1);
        assertThat(userStates.getFirst().userId()).isEqualTo(userId.toString());
        assertThat(userStates.getFirst().purchasedValue()).isEqualTo(4000L);
        assertThat(userStates.getFirst().portfolioCount()).isEqualTo(2L);
        assertThat(userStates.getFirst().portfolioIds()).containsExactly(1L, 2L);
        verify(entityManager, times(2)).clear();
    }

    @Test
    void doesNotLoadAllPortfoliosAtOnce() {
        when(portfolioGroupRepository.findPortfolioIdsAfter(null, 2)).thenReturn(List.of());
        when(portfolioGroupRepository.findUserIdsAfter(null, 2)).thenReturn(List.of());

        service.warmUp();

        verify(portfolioGroupRepository, times(0)).findAllWithAssets();
        verify(valuationWarmUpRedisRepository, times(0)).writePortfolioStates(anyList());
        verify(valuationWarmUpRedisRepository, times(0)).writeUserStates(anyList());
    }

    private Asset asset(Long stockId, String amount, String purchasePriceAmount) {
        return Asset.builder()
            .stockId(stockId)
            .amount(new BigDecimal(amount))
            .totalPrice(Money.of(new BigDecimal(purchasePriceAmount), Currency.KRW))
            .build();
    }
}
