package depth.finvibe.modules.asset.application.port.out;

import java.util.List;

import depth.finvibe.modules.asset.application.dto.PortfolioWarmUpState;
import depth.finvibe.modules.asset.application.dto.UserWarmUpState;

public interface ValuationWarmUpRedisRepository {
    void writePortfolioStates(List<PortfolioWarmUpState> states);

    void writeUserStates(List<UserWarmUpState> states);
}
