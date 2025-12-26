package com.enit.catalog.service;

import com.enit.catalog.dto.request.ProductRequestDTO;
import com.enit.catalog.dto.response.PageResponseDTO;
import com.enit.catalog.dto.response.ProductResponseDTO;

import java.util.List;

public interface ProductService {
    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO);
    ProductResponseDTO getProductById(Long id);
    List<ProductResponseDTO> getAllProducts();
    PageResponseDTO<ProductResponseDTO> getAllProductsPaginated(int page, int size, String sortBy, String sortDirection);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO productRequestDTO);
    void deleteProduct(Long id);
    ProductResponseDTO getProductByName(String name);
}
