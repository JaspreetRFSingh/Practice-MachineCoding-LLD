package HitCounter;

import java.util.concurrent.atomic.AtomicInteger;

interface WebpageVisitCounterInterface {
    void init(int totalPages);
    void incrementVisitCount(int pageIndex);
    int getVisitCount(int pageIndex);
}

public class HitCounter implements WebpageVisitCounterInterface {

    private AtomicInteger[] counts;
    private int totalPages;

    @Override
    public void init(int totalPages) {
        this.totalPages = totalPages;
        counts = new AtomicInteger[totalPages];
        for (int i = 0; i < totalPages; i++) counts[i] = new AtomicInteger(0);
    }

    @Override
    public void incrementVisitCount(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages)
            throw new IllegalArgumentException("pageIndex out of bounds");
        counts[pageIndex].incrementAndGet();
    }

    @Override
    public int getVisitCount(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages)
            throw new IllegalArgumentException("pageIndex out of bounds");
        return counts[pageIndex].get();
    }
}
