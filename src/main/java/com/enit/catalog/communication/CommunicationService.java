package com.enit.catalog.communication;

import com.enit.catalog.dto.request.RequestSearch;
import com.enit.catalog.dto.response.ResponseSearch;

import java.util.List;

public interface CommunicationService {
    
    // ========== Single Operations ==========
    
    ResponseSearch syncCreate(RequestSearch request);

    ResponseSearch syncUpdate(RequestSearch request);

    ResponseSearch syncDelete(RequestSearch request);

    // ========== Batch Operations ==========
    
    ResponseSearch syncCreateBatch(List<RequestSearch> requests);

    ResponseSearch syncUpdateBatch(List<RequestSearch> requests);

    ResponseSearch syncDeleteBatch(List<RequestSearch> requests);
}
