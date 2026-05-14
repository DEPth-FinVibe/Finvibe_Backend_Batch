package depth.finvibe.modules.asset.infra.redis;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PortfolioStateRedisRepository {

	private static final String OWNER_FIELD = "u";

	private final StringRedisTemplate redisTemplate;

	public void setOwner(Long portfolioId, UUID userId) {
		redisTemplate.opsForHash().put(key(portfolioId), OWNER_FIELD, userId.toString());
	}

	public UUID getOwner(Long portfolioId) {
		Object value = redisTemplate.opsForHash().get(key(portfolioId), OWNER_FIELD);
		if (value == null) {
			return null;
		}
		return UUID.fromString(value.toString());
	}

	private String key(Long portfolioId) {
		return "pf:" + portfolioId;
	}
}
