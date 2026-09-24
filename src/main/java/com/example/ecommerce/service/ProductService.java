package com.example.ecommerce.service;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get product by ID
    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );
    }

    // Add product
    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    // Search products
    public List<Product> searchProducts(String name) {

        return productRepository
                .findByNameContainingIgnoreCase(name);
    }

    // Update product
    public Product updateProduct(
            Long id,
            Product updatedProduct) {

        Product existingProduct =
                getProductById(id);

        existingProduct.setName(
                updatedProduct.getName()
        );

        existingProduct.setDescription(
                updatedProduct.getDescription()
        );

        existingProduct.setPrice(
                updatedProduct.getPrice()
        );

        existingProduct.setStock(
                updatedProduct.getStock()
        );

        existingProduct.setCategory(
                updatedProduct.getCategory()
        );

        existingProduct.setImageUrl(
                updatedProduct.getImageUrl()
        );

        return productRepository.save(existingProduct);
    }

    // Delete product
    public void deleteProduct(Long id) {

        Product product =
                getProductById(id);

        productRepository.delete(product);
    }
}

