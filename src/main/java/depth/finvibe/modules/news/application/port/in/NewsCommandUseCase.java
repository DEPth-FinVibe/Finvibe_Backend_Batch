package depth.finvibe.modules.news.application.port.in;

public interface NewsCommandUseCase {
    void syncLatestNews();

    void syncAllDiscussionCounts();
}
