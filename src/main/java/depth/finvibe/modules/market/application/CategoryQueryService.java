package depth.finvibe.modules.market.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.market.application.port.in.CategoryQueryUseCase;
import depth.finvibe.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.modules.market.application.port.out.StockRepository;
import depth.finvibe.modules.market.domain.BatchUpdatePrice;
import depth.finvibe.modules.market.domain.Category;
import depth.finvibe.modules.market.domain.Stock;
import depth.finvibe.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.modules.market.dto.CategoryDto;
import depth.finvibe.modules.market.dto.CategoryInternalDto;
import depth.finvibe.common.error.DomainException;

@Service
@RequiredArgsConstructor
public class CategoryQueryService implements CategoryQueryUseCase {

    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;
    private final BatchUpdatePriceRepository batchUpdatePriceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryInternalDto.Response> getAllCategoriesForInternal() {
        return categoryRepository.findAll().stream()
                .map(CategoryInternalDto.Response::of)
                .toList();
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = DomainException.class)
    public CategoryDto.ChangeRateResponse getCategoryChangeRate(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new DomainException(MarketErrorCode.CATEGORY_NOT_FOUND));

        List<Stock> stocks = stockRepository.findByCategoryId(categoryId);
        if (stocks.isEmpty()) {
            throw new DomainException(MarketErrorCode.NO_STOCKS_IN_CATEGORY);
        }

        List<Long> stockIds = stocks.stream().map(Stock::getId).toList();
        List<BatchUpdatePrice> batchPrices = batchUpdatePriceRepository.findByStockIds(stockIds);
        if (batchPrices.isEmpty()) {
            throw new DomainException(MarketErrorCode.NO_PRICE_DATA_AVAILABLE);
        }

        List<BatchUpdatePrice> pricesWithChange = batchPrices.stream()
                .filter(price -> price.getPrevDayChangePct() != null)
                .toList();
        if (pricesWithChange.isEmpty()) {
            throw new DomainException(MarketErrorCode.NO_PRICE_DATA_AVAILABLE);
        }

        BigDecimal sum = pricesWithChange.stream()
                .map(BatchUpdatePrice::getPrevDayChangePct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(
                BigDecimal.valueOf(pricesWithChange.size()),
                4,
                RoundingMode.HALF_UP
        );

        int positiveCount = (int) pricesWithChange.stream()
                .filter(price -> price.getPrevDayChangePct().compareTo(BigDecimal.ZERO) > 0)
                .count();
        int negativeCount = (int) pricesWithChange.stream()
                .filter(price -> price.getPrevDayChangePct().compareTo(BigDecimal.ZERO) < 0)
                .count();

        LocalDateTime updatedAt = batchPrices.stream()
                .map(BatchUpdatePrice::getAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return CategoryDto.ChangeRateResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .averageChangePct(average)
                .stockCount(pricesWithChange.size())
                .positiveCount(positiveCount)
                .negativeCount(negativeCount)
                .updatedAt(updatedAt)
                .build();
    }
}
