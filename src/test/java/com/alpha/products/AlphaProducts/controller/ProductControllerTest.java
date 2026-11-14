package com.alpha.products.AlphaProducts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alpha.products.AlphaProducts.exception.ProductNotFoundException;
import com.alpha.products.AlphaProducts.model.response.GetProduct;
import com.alpha.products.AlphaProducts.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired MockMvc mvc;
  @MockBean ProductService service;

  private GetProduct sample() {
    return new GetProduct("id-1", "089686386219", "BUMBU RACIK TUMIS", BigDecimal.valueOf(3500), "PCS", null);
  }

  @Test
  void barcodeOkWithoutGenerateParam() throws Exception {
    when(service.getProductByBarcode("089686386219", false)).thenReturn(sample());
    mvc.perform(get("/api/v1/products-barcode").param("barcode", "089686386219"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("BUMBU RACIK TUMIS"));
  }

  @Test
  void barcodeNotFoundIs404() throws Exception {
    when(service.getProductByBarcode("999", false)).thenThrow(new ProductNotFoundException("999"));
    mvc.perform(get("/api/v1/products-barcode").param("barcode", "999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void barcodeInvalidCharsIs400() throws Exception {
    mvc.perform(get("/api/v1/products-barcode").param("barcode", "12;DROP"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingBarcodeIs400() throws Exception {
    mvc.perform(get("/api/v1/products-barcode")).andExpect(status().isBadRequest());
  }

  @Test
  void searchReturnsArrayAndTotalHeaders() throws Exception {
    when(service.getProductsByName(eq("bumbu"), any()))
        .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 50), 1));
    mvc.perform(get("/api/v1/products").param("name", "bumbu"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "1"))
        .andExpect(jsonPath("$[0].barcode").value("089686386219"));
  }

  @Test
  void searchShortNameIs400() throws Exception {
    mvc.perform(get("/api/v1/products").param("name", "a")).andExpect(status().isBadRequest());
  }

  @Test
  void searchSizeTooLargeIs400() throws Exception {
    mvc.perform(get("/api/v1/products").param("name", "bumbu").param("size", "1000"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void corsPreflightAllowed() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .options("/api/v1/products")
            .header("Origin", "https://user.github.io")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Access-Control-Allow-Origin"));
  }
}
