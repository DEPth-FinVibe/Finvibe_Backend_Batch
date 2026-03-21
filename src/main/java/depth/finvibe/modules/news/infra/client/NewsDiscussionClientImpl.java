package depth.finvibe.modules.news.infra.client;

import depth.finvibe.modules.discussion.application.port.in.DiscussionQueryUseCase;
import depth.finvibe.modules.news.application.port.out.NewsDiscussionPort;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsDiscussionClientImpl implements NewsDiscussionPort {

    private final DiscussionQueryUseCase discussionQueryUseCase;

    @Override
    public Map<Long, Long> getDiscussionCounts(List<Long> newsIds) {
        return discussionQueryUseCase.countByNewsIds(newsIds);
    }
}
