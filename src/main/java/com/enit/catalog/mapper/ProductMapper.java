package com.enit.catalog.mapper;

import com.enit.catalog.dto.request.ProductRequestDTO;
import com.enit.catalog.dto.response.PageResponseDTO;
import com.enit.catalog.dto.response.ProductResponseDTO;
import com.enit.catalog.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
    
    ProductResponseDTO toResponseDTO(Product product);
    
    Product toEntity(ProductRequestDTO requestDTO);
    
    void updateEntityFromRequestDTO(ProductRequestDTO requestDTO, @MappingTarget Product product);
    
    default PageResponseDTO<ProductResponseDTO> toPageResponseDTO(Page<Product> page) {
        List<ProductResponseDTO> content = page.getContent()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        
        return PageResponseDTO.<ProductResponseDTO>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
