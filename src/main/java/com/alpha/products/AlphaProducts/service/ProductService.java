package com.alpha.products.AlphaProducts.service;

import com.alpha.products.AlphaProducts.model.response.GetProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
  Page<GetProduct> getProductsByName(String name, Pageable pageable);
  GetProduct getProductByBarcode(String barcode, boolean generateBarcode);
}
