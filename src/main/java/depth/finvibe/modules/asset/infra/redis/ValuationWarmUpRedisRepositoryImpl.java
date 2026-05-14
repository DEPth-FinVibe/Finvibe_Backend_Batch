package depth.finvibe.modules.asset.infra.redis;

import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.dto.PortfolioWarmUpState;
import depth.finvibe.modules.asset.application.dto.UserWarmUpState;
import depth.finvibe.modules.asset.application.port.out.ValuationWarmUpRedisRepository;

@Repository
@RequiredArgsConstructor
public class ValuationWarmUpRedisRepositoryImpl implements ValuationWarmUpRedisRepository {
    private static final String LEGACY_STOCK_HOLDING_PREFIX = "stock:holding:";
    private static final String LEGACY_STOCK_HOLDING_SUFFIX = ":portfolios";
    private static final String LEGACY_PORTFOLIO_ASSETS_PREFIX = "portfolio:assets:";
    private static final String LEGACY_PORTFOLIO_OWNER_PREFIX = "portfolio:owner:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void writePortfolioStates(List<PortfolioWarmUpState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (PortfolioWarmUpState state : states) {
                writePortfolioState(connection, state);
            }
            return null;
        });
    }

    @Override
    public void writeUserStates(List<UserWarmUpState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (UserWarmUpState state : states) {
                writeUserState(connection, state);
            }
            return null;
        });
    }

    private void writePortfolioState(RedisConnection connection, PortfolioWarmUpState state) {
        byte[] portfolioId = bytes(state.portfolioId());
        byte[] userId = bytes(state.userId());

        connection.keyCommands().del(bytes(legacyPortfolioAssetsKey(state.portfolioId())));
        connection.keyCommands().del(bytes(portfolioStocksKey(state.portfolioId())));
        set(connection, legacyPortfolioOwnerKey(state.portfolioId()), state.userId());
        hSet(connection, portfolioHashKey(state.portfolioId()), "u", state.userId());
        hSet(connection, portfolioHashKey(state.portfolioId()), "pv", state.purchasedValue());
        hSet(connection, portfolioHashKey(state.portfolioId()), "cv", state.currentValue());
        hSet(connection, portfolioHashKey(state.portfolioId()), "pr", state.profitRate());
        hSet(connection, portfolioHashKey(state.portfolioId()), "ac", state.assetCount());
        hSet(connection, portfolioHashKey(state.portfolioId()), "del", "0");
        hSet(connection, portfolioHashKey(state.portfolioId()), "ua", state.updatedAt().toString());

        for (PortfolioWarmUpState.Holding holding : state.holdings()) {
            byte[] stockId = bytes(holding.stockId());
            connection.setCommands().sAdd(bytes(legacyStockHoldingKey(holding.stockId())), portfolioId);
            connection.setCommands().sAdd(bytes(stockPortfoliosKey(holding.stockId())), portfolioId);
            connection.setCommands().sAdd(bytes(portfolioStocksKey(state.portfolioId())), stockId);
            connection.hashCommands().hSet(
                bytes(legacyPortfolioAssetsKey(state.portfolioId())),
                stockId,
                bytes(holding.quantity().toPlainString() + "|" + holding.purchasePriceAmount().toPlainString() + "|" + holding.currency())
            );
            set(connection, portfolioStockKey(state.portfolioId(), holding.stockId(), "quantity"), holding.quantity().toPlainString());
        }

        connection.setCommands().sAdd(bytes(userPortfoliosKey(state.userId())), portfolioId);
    }

    private void writeUserState(RedisConnection connection, UserWarmUpState state) {
        connection.keyCommands().del(bytes(userPortfoliosKey(state.userId())));
        for (Long portfolioId : state.portfolioIds()) {
            connection.setCommands().sAdd(bytes(userPortfoliosKey(state.userId())), bytes(portfolioId));
        }
        hSet(connection, userHashKey(state.userId()), "pv", state.purchasedValue());
        hSet(connection, userHashKey(state.userId()), "cv", state.currentValue());
        hSet(connection, userHashKey(state.userId()), "pr", state.profitRate());
        hSet(connection, userHashKey(state.userId()), "pc", state.portfolioCount());
        hSet(connection, userHashKey(state.userId()), "ua", state.updatedAt().toString());
    }

    private void set(RedisConnection connection, String key, Object value) {
        connection.stringCommands().set(bytes(key), bytes(String.valueOf(value)));
    }

    private void hSet(RedisConnection connection, String key, String field, Object value) {
        connection.hashCommands().hSet(bytes(key), bytes(field), bytes(value));
    }

    private String legacyStockHoldingKey(Long stockId) {
        return LEGACY_STOCK_HOLDING_PREFIX + stockId + LEGACY_STOCK_HOLDING_SUFFIX;
    }

    private String legacyPortfolioAssetsKey(Long portfolioId) {
        return LEGACY_PORTFOLIO_ASSETS_PREFIX + portfolioId;
    }

    private String legacyPortfolioOwnerKey(Long portfolioId) {
        return LEGACY_PORTFOLIO_OWNER_PREFIX + portfolioId;
    }

    private String stockPortfoliosKey(Long stockId) {
        return "stock:" + stockId + ":portfolios";
    }

    private String portfolioStocksKey(Long portfolioId) {
        return "portfolio:" + portfolioId + ":stocks";
    }

    private String portfolioStockKey(Long portfolioId, Long stockId, String field) {
        return "portfolio:" + portfolioId + ":stock:" + stockId + ":" + field;
    }

    private String portfolioHashKey(Long portfolioId) {
        return "pf:" + portfolioId;
    }

    private String userHashKey(String userId) {
        return "usr:" + userId;
    }

    private String userPortfoliosKey(String userId) {
        return "user:" + userId + ":portfolios";
    }

    private byte[] bytes(Object value) {
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }
}
