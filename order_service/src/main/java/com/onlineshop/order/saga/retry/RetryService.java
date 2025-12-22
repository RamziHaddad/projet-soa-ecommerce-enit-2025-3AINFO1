package com.onlineshop.order.saga.retry;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetryService {

    private final SagaStateRepository sagaStateRepository;
    private final RetryConfiguration retryConfiguration;

    private final Random random = new Random();

    public boolean canRetry(SagaState sagaState) {
        if (sagaState == null) {
            return false;
        }

        boolean isRetryableState = sagaState.getStatus() == com.onlineshop.order.model.SagaStatus.FAILED ||
                sagaState.getStatus() == com.onlineshop.order.model.SagaStatus.IN_PROGRESS;

        boolean withinMaxRetries = sagaState.getRetryCount() < getMaxRetries();

        return isRetryableState && withinMaxRetries;
    }

    public void prepareForRetry(SagaState sagaState) {
        if (sagaState == null) {
            throw new IllegalArgumentException("SagaState cannot be null");
        }

        int currentRetryCount = sagaState.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;

        LocalDateTime nextRetryTime = calculateNextRetryTime(nextRetryCount);

        sagaState.setRetryCount(nextRetryCount);
        sagaState.setLastRetryTime(LocalDateTime.now());
        sagaState.setNextRetryTime(nextRetryTime);
        sagaState.setStatus(com.onlineshop.order.model.SagaStatus.IN_PROGRESS);

        sagaStateRepository.save(sagaState);
    }

    public LocalDateTime calculateNextRetryTime(int retryCount) {

        long baseDelaySeconds = (long) Math.pow(2, retryCount - 1);

        long maxDelaySeconds = retryConfiguration.getMaxRetryDelaySeconds();
        long delaySeconds = Math.min(baseDelaySeconds, maxDelaySeconds);

        double jitterFactor = 0.8 + (0.4 * random.nextDouble());
        long jitteredDelaySeconds = (long) (delaySeconds * jitterFactor);

        return LocalDateTime.now().plusSeconds(jitteredDelaySeconds);
    }

    public int getMaxRetries() {
        return retryConfiguration.getMaxRetries();
    }

    public void markRetryFailed(SagaState sagaState, Exception exception) {
        sagaState.setStatus(com.onlineshop.order.model.SagaStatus.FAILED);
        sagaState.setErrorMessage(exception.getMessage());
        sagaState.setLastErrorStackTrace(getStackTraceAsString(exception));
        sagaState.setLastRetryTime(LocalDateTime.now());

        sagaStateRepository.save(sagaState);
    }

    private String getStackTraceAsString(Exception exception) {
        if (exception == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");

        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }

        return sb.toString();
    }
}
