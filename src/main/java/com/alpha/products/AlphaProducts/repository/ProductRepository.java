package com.alpha.products.AlphaProducts.repository;

import com.alpha.products.AlphaProducts.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
  Optional<Product> findFirstByBarcode(String barcode);
  Page<Product> findAllByNameIsContainingIgnoreCase(String name, Pageable pageable);
}
