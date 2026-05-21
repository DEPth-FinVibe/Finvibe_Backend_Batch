package depth.finvibe.modules.asset.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PortfolioOwnerRedisRepository {

	private static final String KEY_PREFIX = "portfolio:owner:";

	private final StringRedisTemplate redisTemplate;

	public void set(Long portfolioId, Long userId) {
		redisTemplate.opsForValue().set(key(portfolioId), userId.toString());
	}

	public Long get(Long portfolioId) {
		String value = redisTemplate.opsForValue().get(key(portfolioId));
		if (value == null) {
			return null;
		}
		return Long.valueOf(value);
	}

	public void delete(Long portfolioId) {
		redisTemplate.delete(key(portfolioId));
	}

	private String key(Long portfolioId) {
		return KEY_PREFIX + portfolioId;
	}
}
