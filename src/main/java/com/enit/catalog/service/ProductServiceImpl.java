package com.enit.catalog.service;

import com.enit.catalog.dto.request.ProductRequestDTO;
import com.enit.catalog.dto.response.PageResponseDTO;
import com.enit.catalog.dto.response.ProductResponseDTO;
import com.enit.catalog.entity.Product;
import com.enit.catalog.exception.ResourceNotFoundException;
import com.enit.catalog.mapper.ProductMapper;
import com.enit.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OutBoxEventService outBoxEventService;
    
    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {
        Product product = productMapper.toEntity(productRequestDTO);
        Product savedProduct = productRepository.save(product);
        
        // Créer un événement Outbox dans la même transaction
        outBoxEventService.createProductCreatedEvent(savedProduct);
        log.info("Produit créé avec ID: {} et événement Outbox enregistré", savedProduct.getId());
        
        return productMapper.toResponseDTO(savedProduct);
    }
    
    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponseDTO(product);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponseDTO<ProductResponseDTO> getAllProductsPaginated(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(pageable);
        
        return productMapper.toPageResponseDTO(productPage);
    }
    
    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO productRequestDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        productMapper.updateEntityFromRequestDTO(productRequestDTO, existingProduct);
        
        Product updatedProduct = productRepository.save(existingProduct);
        
        // Créer un événement Outbox dans la même transaction
        outBoxEventService.createProductUpdatedEvent(updatedProduct);
        return productMapper.toResponseDTO(updatedProduct);
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Créer un événement Outbox AVANT la suppression dans la même transaction
        outBoxEventService.createProductDeletedEvent(id);
        
        productRepository.delete(product);
    }
    
    @Override
    public ProductResponseDTO getProductByName(String name) {
        Product product = productRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with name: " + name));
        return productMapper.toResponseDTO(product);
    }
}
