package depth.finvibe.modules.asset.application.port.out;

import java.util.List;

import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.domain.UserValuation;

public interface UserValuationRepository {
    void upsertIfNewer(UserValuationSnapshot snapshot);

    List<UserValuation> findAfter(String lastUserId, int limit);
}
