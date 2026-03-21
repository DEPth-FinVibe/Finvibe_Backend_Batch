package depth.finvibe.modules.news.application.port.out;

import java.util.List;
import java.util.Map;

public interface NewsDiscussionPort {
    Map<Long, Long> getDiscussionCounts(List<Long> newsIds);
}
