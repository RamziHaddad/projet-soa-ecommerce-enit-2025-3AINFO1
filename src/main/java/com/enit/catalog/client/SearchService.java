package com.enit.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.enit.catalog.dto.request.RequestSearch;
import com.enit.catalog.dto.response.ResponseSearch;

import java.util.List;

@FeignClient(name = "search-service", url = "${search.service.url}")
public interface SearchService {
    
    // ========== Single Operations ==========
    
    @PostMapping("/api/search/index")
    ResponseSearch indexData(@RequestBody RequestSearch requestSearch);

    @DeleteMapping("/api/search/delete")
    ResponseSearch deleteData(@RequestBody RequestSearch requestSearch);

    @PatchMapping("/api/search/update")
    ResponseSearch updateData(@RequestBody RequestSearch requestSearch);
    
    // ========== Batch Operations ==========
    
    @PostMapping("/api/search/index/batch")
    ResponseSearch indexDataBatch(@RequestBody List<RequestSearch> requests);

    @DeleteMapping("/api/search/delete/batch")
    ResponseSearch deleteDataBatch(@RequestBody List<RequestSearch> requests);

    @PatchMapping("/api/search/update/batch")
    ResponseSearch updateDataBatch(@RequestBody List<RequestSearch> requests);
}
