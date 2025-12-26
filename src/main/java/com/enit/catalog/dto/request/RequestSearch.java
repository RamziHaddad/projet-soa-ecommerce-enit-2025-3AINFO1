package com.enit.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestSearch {
    private Long productId;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
}
