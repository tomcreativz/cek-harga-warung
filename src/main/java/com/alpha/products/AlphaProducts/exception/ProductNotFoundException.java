package com.alpha.products.AlphaProducts.exception;

public class ProductNotFoundException extends RuntimeException {
  public ProductNotFoundException(String barcode) {
    super("Produk dengan barcode '" + barcode + "' tidak ditemukan");
  }
}
