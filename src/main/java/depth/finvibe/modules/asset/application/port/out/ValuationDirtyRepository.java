package depth.finvibe.modules.asset.application.port.out;

import java.util.List;

public interface ValuationDirtyRepository {
    void addPortfolioValuationDirty(List<Long> portfolioIds);

    void addUserValuationDirty(List<String> userIds);

    List<Long> scanPortfolioValuationDirty(int limit);

    List<String> scanUserValuationDirty(int limit);

    List<Long> scanPortfolioValuationDeletionDirty(int limit);

    void removePortfolioValuationDirty(Long portfolioId);

    void removeUserValuationDirty(String userId);

    void removePortfolioValuationDeletionDirty(Long portfolioId);

    long countPortfolioValuationDirty();

    long countUserValuationDirty();

    long countPortfolioValuationDeletionDirty();
}
