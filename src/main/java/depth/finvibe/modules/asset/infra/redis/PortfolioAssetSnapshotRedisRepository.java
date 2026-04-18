package depth.finvibe.modules.asset.infra.redis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PortfolioAssetSnapshotRedisRepository {

	private static final String KEY_PREFIX = "portfolio:assets:";
	private static final String SEPARATOR = "|";

	private final StringRedisTemplate redisTemplate;

	public void replaceAll(Long portfolioId, Map<Long, AssetSnapshot> assets) {
		String k = key(portfolioId);
		redisTemplate.delete(k);
		if (assets != null && !assets.isEmpty()) {
			Map<String, String> hash = new HashMap<>(assets.size());
			for (Map.Entry<Long, AssetSnapshot> entry : assets.entrySet()) {
				AssetSnapshot s = entry.getValue();
				hash.put(entry.getKey().toString(), encode(s.amount(), s.purchasePriceAmount(), s.currency()));
			}
			redisTemplate.opsForHash().putAll(k, hash);
		}
	}

	public Map<Long, AssetSnapshot> getAssets(Long portfolioId) {
		Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(portfolioId));
		if (entries.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, AssetSnapshot> result = new HashMap<>(entries.size());
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			Long stockId = Long.valueOf(entry.getKey().toString());
			AssetSnapshot snapshot = decode(entry.getValue().toString());
			if (snapshot != null) {
				result.put(stockId, snapshot);
			}
		}
		return result;
	}

	public void deleteAll(Long portfolioId) {
		redisTemplate.delete(key(portfolioId));
	}

	private String key(Long portfolioId) {
		return KEY_PREFIX + portfolioId;
	}

	private String encode(BigDecimal amount, BigDecimal purchasePriceAmount, String currency) {
		return amount.toPlainString() + SEPARATOR + purchasePriceAmount.toPlainString() + SEPARATOR + currency;
	}

	private AssetSnapshot decode(String value) {
		String[] parts = value.split("\\|", 3);
		if (parts.length != 3) {
			log.warn("Invalid asset snapshot format: {}", value);
			return null;
		}
		return new AssetSnapshot(
				new BigDecimal(parts[0]),
				new BigDecimal(parts[1]),
				parts[2]
		);
	}

	public record AssetSnapshot(BigDecimal amount, BigDecimal purchasePriceAmount, String currency) {
	}
}
