package depth.finvibe.modules.news.infra.persistence;

import depth.finvibe.modules.news.application.port.out.NewsRepository;
import depth.finvibe.modules.news.domain.News;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryAdapter implements NewsRepository {

    private final NewsJpaRepository newsJpaRepository;
    private final EntityManager entityManager;

    @Override
    public News save(News news) {
        return newsJpaRepository.save(news);
    }

    @Override
    public List<News> findAll() {
        return newsJpaRepository.findAll();
    }

    @Override
    public Optional<News> findById(Long id) {
        return newsJpaRepository.findById(id);
    }

    @Override
    public boolean existsByTitle(String title) {
        return newsJpaRepository.existsByTitle(title);
    }

    @Override
    public Set<String> findExistingTitlesIn(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return Set.of();
        }
        return newsJpaRepository.findExistingTitlesIn(titles);
    }

    @Override
    public List<News> findPageAfterId(Long lastNewsId, int limit) {
        long cursor = lastNewsId == null ? 0L : lastNewsId;
        return newsJpaRepository.findByIdGreaterThanOrderByIdAsc(cursor, PageRequest.of(0, limit)).getContent();
    }

    @Override
    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public org.springframework.data.domain.Page<News> findAll(org.springframework.data.domain.Pageable pageable) {
        return newsJpaRepository.findAll(pageable);
    }

    @Override
    public org.springframework.data.domain.Page<News> findAllOrderByPublishedAtDescIdDesc(
            org.springframework.data.domain.Pageable pageable) {
        return newsJpaRepository.findAllByOrderByPublishedAtDescIdDesc(pageable);
    }

    @Override
    public List<News> findAllByCreatedAtAfter(LocalDateTime createdAfter) {
        return newsJpaRepository.findAllByCreatedAtAfter(createdAfter);
    }

    @Override
    public List<News> findAllByCategoryIdAndPublishedAtBetweenOrderByPublishedAtDesc(
            Long categoryId,
            LocalDateTime start,
            LocalDateTime end) {
        return newsJpaRepository.findAllByCategoryIdAndPublishedAtBetweenOrderByPublishedAtDesc(
                categoryId,
                start,
                end);
    }

    @Override
    public List<NewsCategoryCount> countByCategoryIdForPeriod(LocalDateTime start, LocalDateTime end) {
        return newsJpaRepository.countByCategoryIdForPeriod(start, end).stream()
                .map(row -> new NewsCategoryCount(row.getCategoryId(), row.getCount()))
                .toList();
    }
}
