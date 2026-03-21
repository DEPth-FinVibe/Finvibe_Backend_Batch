package depth.finvibe.modules.discussion.application.port.in;

import java.util.List;
import java.util.Map;

public interface DiscussionQueryUseCase {
    Map<Long, Long> countByNewsIds(List<Long> newsIds);
}
