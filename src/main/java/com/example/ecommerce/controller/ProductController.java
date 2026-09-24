package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import com.example.ecommerce.dto.ProductRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    public ProductController(
            ProductService productService,
            CloudinaryService cloudinaryService) {

        this.productService = productService;
        this.cloudinaryService = cloudinaryService;
    }

    // Get all products
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {

        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    // Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    // Search products
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String name) {

        return ResponseEntity.ok(
                productService.searchProducts(name)
        );
    }

    @PostMapping("/json")
    public ResponseEntity<Product> addProductJson(
            @Valid @RequestBody ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        return ResponseEntity.ok(
                productService.addProduct(product)
        );
    }
    @PostMapping(
            value = "/admin",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Product> addProduct(

            @Valid @ModelAttribute ProductRequest request,

            @RequestParam("image")
            MultipartFile image

    ) throws IOException {

        String imageUrl =
                cloudinaryService.uploadImage(image);

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(imageUrl);

        return ResponseEntity.ok(
                productService.addProduct(product)
        );
    }

    @PutMapping(
            value = "/admin/{id}",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Product> updateProduct(

            @PathVariable Long id,

            @RequestParam("name")
            String name,

            @RequestParam("description")
            String description,

            @RequestParam("price")
            double price,

            @RequestParam("stock")
            int stock,

            @RequestParam(value = "image", required = false)
            MultipartFile image

    ) throws IOException {

        Product existingProduct =
                productService.getProductById(id);

        existingProduct.setName(name);
        existingProduct.setDescription(description);
        existingProduct.setPrice(price);
        existingProduct.setStock(stock);

        if (image != null && !image.isEmpty()) {

            String imageUrl =
                    cloudinaryService.uploadImage(image);

            existingProduct.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(
                productService.addProduct(existingProduct)
        );
    }


    // Delete product
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                "Product deleted successfully"
        );
    }
}

