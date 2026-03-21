package depth.finvibe.modules.discussion.application;

import depth.finvibe.modules.discussion.application.port.in.DiscussionQueryUseCase;
import depth.finvibe.modules.discussion.application.port.out.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscussionQueryService implements DiscussionQueryUseCase {

    private final DiscussionRepository discussionRepository;

    @Override
    public Map<Long, Long> countByNewsIds(List<Long> newsIds) {
        return discussionRepository.countByNewsIds(newsIds);
    }
}
