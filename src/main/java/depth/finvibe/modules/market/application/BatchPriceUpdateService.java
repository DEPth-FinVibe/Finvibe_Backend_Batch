package depth.finvibe.modules.market.application;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import depth.finvibe.modules.market.application.port.out.BatchPriceEventProducer;
import depth.finvibe.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.modules.market.application.port.out.StockRepository;
import depth.finvibe.modules.market.domain.BatchUpdatePrice;
import depth.finvibe.modules.market.domain.Category;
import depth.finvibe.modules.market.domain.Stock;
import depth.finvibe.modules.market.dto.PriceCandleDto;
import depth.finvibe.common.investment.dto.BatchPriceUpdatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPriceUpdateService {

  private static final String EXCLUDED_CATEGORY_NAME = "기타";

  private final HoldingStockRepository holdingStockRepository;
  private final StockRepository stockRepository;
  private final CategoryRepository categoryRepository;
  private final RealMarketClient realMarketClient;
  private final BatchUpdatePriceRepository batchUpdatePriceRepository;
  private final BatchPriceEventProducer batchPriceEventProducer;

  public boolean hasMissingBatchPricesByKey() {
    List<Long> targetStockIds = findTargetStockIds();
    if (targetStockIds.isEmpty()) {
      return false;
    }

    int cachedCount = batchUpdatePriceRepository.findByStockIds(targetStockIds).size();
    boolean hasMissing = cachedCount < targetStockIds.size();

    if (hasMissing) {
      log.info("Detected missing batch price keys. target: {}, cached: {}", targetStockIds.size(), cachedCount);
    }

    return hasMissing;
  }

  public void updateHoldingStockPrices() {
    log.info("Starting batch price update for holding stocks");

    List<Long> holdingStockIds = holdingStockRepository.findAllDistinctStockIds();
    List<Long> categoryStockIds = findCategoryStockIdsExcludingMisc();
    List<Long> targetStockIds = mergeStockIds(holdingStockIds, categoryStockIds);
    if (targetStockIds.isEmpty()) {
      log.info("No target stocks found. Skipping batch price update.");
      return;
    }

    log.info("Found {} target stocks (holding: {}, category: {})",
            targetStockIds.size(),
            holdingStockIds.size(),
            categoryStockIds.size());

    List<BatchUpdatePrice> batchPrices = new ArrayList<>();
    bulkFetchPricesFromMarket(batchPrices, targetStockIds);

    if (batchPrices.isEmpty()) {
      log.warn("No price data available for batch update");
      return;
    }

    try {
      batchUpdatePriceRepository.saveAll(batchPrices);

      log.info("Successfully saved {} batch price updates to Redis", batchPrices.size());

      BatchPriceUpdatedEvent event = BatchPriceUpdatedEvent.builder()
              .batchExecutedAt(LocalDateTime.now())
              .totalStockCount(batchPrices.size())
              .updatedStockIds(batchPrices.stream().map(BatchUpdatePrice::getStockId).toList())
              .build();
      batchPriceEventProducer.publishBatchPriceUpdated(event);
      log.info("Published batch price updated event for {} stocks", batchPrices.size());
    } catch (Exception e) {
      log.error("Failed to update batch prices for holding stocks", e);
      throw e;
    }
  }

  private List<Long> mergeStockIds(List<Long> baseIds, List<Long> newIds) {
    Set<Long> merged = new java.util.LinkedHashSet<>(baseIds);
    merged.addAll(newIds);
    return new ArrayList<>(merged);
  }

  private List<Long> findTargetStockIds() {
    List<Long> holdingStockIds = holdingStockRepository.findAllDistinctStockIds();
    List<Long> categoryStockIds = findCategoryStockIdsExcludingMisc();
    return mergeStockIds(holdingStockIds, categoryStockIds);
  }

  private List<Long> findCategoryStockIdsExcludingMisc() {
    return categoryRepository.findByName(EXCLUDED_CATEGORY_NAME)
            .map(Category::getId)
            .map(stockRepository::findAllCategoryStockIdsExcluding)
            .orElseGet(stockRepository::findAllCategoryStockIds);
  }

  private void bulkFetchPricesFromMarket(List<BatchUpdatePrice> batchPrices, List<Long> stockIds) {
    List<String> symbols = fetchSymbols(stockIds);
    if (symbols.isEmpty()) {
      log.warn("No valid stock symbols found for batch update targets");
      return;
    }

    try {
      List<PriceCandleDto.Response> priceResponses = realMarketClient.bulkFetchCurrentPrices(symbols);

      if (priceResponses.isEmpty()) {
        log.warn("No price data returned from market client");
        return;
      }

      log.info("Fetched {} price updates from market client", priceResponses.size());

      List<BatchUpdatePrice> apiBatchPrices = priceResponses.stream()
              .map(BatchUpdatePrice::from)
              .toList();

      batchPrices.addAll(apiBatchPrices);
    } catch (Exception e) {
      log.error("Failed to update batch prices for holding stocks", e);
      throw e;
    }
  }

  private List<String> fetchSymbols(List<Long> stockIds) {
    List<Stock> stocks = stockRepository.findAllById(stockIds);
    Map<Long, String> stockIdToSymbolMap = stocks.stream()
            .collect(Collectors.toMap(Stock::getId, Stock::getSymbol));

    return stockIds.stream()
            .map(stockIdToSymbolMap::get)
            .filter(symbol -> symbol != null && !symbol.isBlank())
            .toList();
  }
}
