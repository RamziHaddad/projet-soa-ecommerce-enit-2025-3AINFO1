package com.enit.catalog.communication;

import com.enit.catalog.client.SearchService;
import com.enit.catalog.dto.request.RequestSearch;
import com.enit.catalog.dto.response.ResponseSearch;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestCommunicationservice implements CommunicationService {

    private final SearchService searchService;

    // ========== Single Operations ==========

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncCreate(RequestSearch request) {
        try {
            return searchService.indexData(request);
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncUpdate(RequestSearch request) {
        try {
            return searchService.updateData(request);
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncDelete(RequestSearch request) {
        try {
            return searchService.deleteData(request);
        } catch (FeignException e) {
            throw e;
        }
    }

    // ========== Batch Operations ==========

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncCreateBatch(List<RequestSearch> requests) {
        if (requests.isEmpty()) {
            return new ResponseSearch("Aucun produit à indexer");
        }
        try {
            return searchService.indexDataBatch(requests);
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncUpdateBatch(List<RequestSearch> requests) {
        if (requests.isEmpty()) {
            return new ResponseSearch("Aucun produit à mettre à jour");
        }
        try {
            return searchService.updateDataBatch(requests);
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    @Retryable(
        retryFor = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseSearch syncDeleteBatch(List<RequestSearch> requests) {
        if (requests.isEmpty()) {
            return new ResponseSearch("Aucun produit à supprimer");
        }
        try {
            return searchService.deleteDataBatch(requests);
        } catch (FeignException e) {
            throw e;
        }
    }
}
