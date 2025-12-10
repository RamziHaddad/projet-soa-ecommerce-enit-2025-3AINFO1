// src/main/java/com/example/searchservice/scheduler/IndexationScheduler.java
package com.example.searchservice.scheduler;

import com.example.searchservice.service.IndexationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IndexationScheduler {

    @Autowired
    private IndexationService indexationService;

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes (300000 ms)
    public void scheduleIndexation() {
        System.out.println("Démarrage de l'indexation programmée...");
        indexationService.indexProducts();
        System.out.println("Indexation programmée terminée.");
    }
}