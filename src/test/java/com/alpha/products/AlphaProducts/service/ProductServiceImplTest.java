package com.alpha.products.AlphaProducts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.alpha.products.AlphaProducts.entity.Product;
import com.alpha.products.AlphaProducts.exception.ProductNotFoundException;
import com.alpha.products.AlphaProducts.model.response.GetProduct;
import com.alpha.products.AlphaProducts.repository.ProductRepository;
import com.alpha.products.AlphaProducts.service.impl.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock ProductRepository repository;
  @InjectMocks ProductServiceImpl service;

  private Product product(String barcode) {
    Product p = new Product(barcode, "BUMBU RACIK TUMIS", BigDecimal.valueOf(3500), "PCS");
    p.setId("id-1");
    return p;
  }

  @Test
  void notFoundThrows() {
    when(repository.findFirstByBarcode("x")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getProductByBarcode("x", false))
        .isInstanceOf(ProductNotFoundException.class);
  }

  @Test
  void withoutImageByDefault() {
    when(repository.findFirstByBarcode("089686386219")).thenReturn(Optional.of(product("089686386219")));
    GetProduct r = service.getProductByBarcode("089686386219", false);
    assertThat(r.getImgBarcode()).isNull();
    assertThat(r.getName()).isEqualTo("BUMBU RACIK TUMIS");
  }

  @Test
  void generatesPngFor12DigitBarcode() {
    when(repository.findFirstByBarcode("089686386219")).thenReturn(Optional.of(product("089686386219")));
    GetProduct r = service.getProductByBarcode("089686386219", true);
    assertThat(r.getImgBarcode()).isNotBlank();
    byte[] bytes = java.util.Base64.getDecoder().decode(r.getImgBarcode());
    assertThat(bytes[1]).isEqualTo((byte) 'P'); // PNG signature
  }

  @Test
  void invalidBarcodeSkipsImageWithoutError() {
    when(repository.findFirstByBarcode("ABC")).thenReturn(Optional.of(product("ABC")));
    assertThat(service.getProductByBarcode("ABC", true).getImgBarcode()).isNull();
  }

  @Test
  void searchMapsPage() {
    var pageable = PageRequest.of(0, 10);
    when(repository.findAllByNameIsContainingIgnoreCase("bumbu", pageable))
        .thenReturn(new PageImpl<>(List.of(product("089686386219")), pageable, 1));
    assertThat(service.getProductsByName("bumbu", pageable).getContent()).hasSize(1);
  }
}
