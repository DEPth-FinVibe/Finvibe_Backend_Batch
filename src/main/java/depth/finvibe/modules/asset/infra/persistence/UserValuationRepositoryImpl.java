package depth.finvibe.modules.asset.infra.persistence;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.UserValuationRepository;
import depth.finvibe.modules.asset.domain.UserValuation;

@Repository
@RequiredArgsConstructor
public class UserValuationRepositoryImpl implements UserValuationRepository {
    private final UserValuationJpaRepository jpaRepository;

    @Override
    public void upsertIfNewer(UserValuationSnapshot snapshot) {
        if (snapshot == null || snapshot.userId() == null || snapshot.userId().isBlank()) {
            return;
        }

        UserValuation valuation = jpaRepository.findById(snapshot.userId())
            .orElseGet(() -> UserValuation.create(
                snapshot.userId(),
                snapshot.purchasedValue(),
                snapshot.currentValue(),
                snapshot.profitRate(),
                snapshot.portfolioCount(),
                snapshot.updatedAt()
            ));

        if (jpaRepository.existsById(snapshot.userId())) {
            valuation.updateIfNewer(
                snapshot.purchasedValue(),
                snapshot.currentValue(),
                snapshot.profitRate(),
                snapshot.portfolioCount(),
                snapshot.updatedAt()
            );
        }

        jpaRepository.save(valuation);
    }

    @Override
    public List<UserValuation> findAfter(String lastUserId, int limit) {
        return jpaRepository.findAfter(lastUserId, PageRequest.of(0, limit));
    }
}
