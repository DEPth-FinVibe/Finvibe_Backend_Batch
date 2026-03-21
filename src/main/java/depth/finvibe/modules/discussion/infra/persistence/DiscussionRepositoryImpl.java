package depth.finvibe.modules.discussion.infra.persistence;

import depth.finvibe.modules.discussion.application.port.out.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DiscussionRepositoryImpl implements DiscussionRepository {

    private final DiscussionJpaRepository discussionJpaRepository;

    @Override
    public Map<Long, Long> countByNewsIds(List<Long> newsIds) {
        return discussionJpaRepository.countByNewsIds(newsIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        DiscussionJpaRepository.NewsCountProjection::getNewsId,
                        DiscussionJpaRepository.NewsCountProjection::getCount));
    }
}
