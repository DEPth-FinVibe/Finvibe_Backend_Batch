package depth.finvibe.modules.discussion.application.port.out;

import java.util.List;
import java.util.Map;

public interface DiscussionRepository {
    Map<Long, Long> countByNewsIds(List<Long> newsIds);
}
