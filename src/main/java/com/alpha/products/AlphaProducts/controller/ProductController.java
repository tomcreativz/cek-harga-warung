package com.alpha.products.AlphaProducts.controller;

import com.alpha.products.AlphaProducts.model.response.GetProduct;
import com.alpha.products.AlphaProducts.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/api/v1"})
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;

  @GetMapping(value = "/products-barcode", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GetProduct> getProductByBarcode(
      @RequestParam
      @NotBlank(message = "barcode wajib diisi")
      @Size(max = 32, message = "barcode maksimal 32 karakter")
      @Pattern(regexp = "^[0-9A-Za-z-]*$", message = "barcode hanya boleh huruf, angka, dan tanda -")
      String barcode,
      @RequestParam(defaultValue = "false") boolean generateBarcode) {
    return ResponseEntity.ok(service.getProductByBarcode(barcode.trim(), generateBarcode));
  }

  /**
   * Respons tetap berupa array JSON seperti sebelumnya. Paging bersifat opsional
   * (default 50 data per halaman); total ada di header X-Total-Count dan X-Total-Pages.
   */
  @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<GetProduct>> getProductByName(
      @RequestParam
      @NotBlank(message = "name wajib diisi")
      @Size(min = 2, max = 100, message = "name harus 2-100 karakter")
      String name,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "page minimal 0") int page,
      @RequestParam(defaultValue = "50")
      @Min(value = 1, message = "size minimal 1")
      @Max(value = 100, message = "size maksimal 100") int size) {
    Page<GetProduct> result = service.getProductsByName(
        name.trim(), PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id"))));
    return ResponseEntity.ok()
        .header("X-Total-Count", String.valueOf(result.getTotalElements()))
        .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
        .body(result.getContent());
  }

}
