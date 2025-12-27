package com.enit.catalog.scheduler;

public interface SearchProvider {
    void processOutboxEvents();

    void recoverStuckEvents();

    void retryFailedEvents();
}
