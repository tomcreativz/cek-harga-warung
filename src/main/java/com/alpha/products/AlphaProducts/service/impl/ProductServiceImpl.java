package com.alpha.products.AlphaProducts.service.impl;

import com.alpha.products.AlphaProducts.entity.Product;
import com.alpha.products.AlphaProducts.exception.ProductNotFoundException;
import com.alpha.products.AlphaProducts.model.response.GetProduct;
import com.alpha.products.AlphaProducts.repository.ProductRepository;
import com.alpha.products.AlphaProducts.service.ProductService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

  private static final Pattern EAN13_INPUT = Pattern.compile("\\d{12,13}");

  private final ProductRepository repository;

  @Override
  public Page<GetProduct> getProductsByName(String name, Pageable pageable) {
    return repository.findAllByNameIsContainingIgnoreCase(name, pageable)
        .map(product -> productMapping(product, false));
  }

  @Override
  public GetProduct getProductByBarcode(String barcode, boolean generateBarcode) {
    return repository.findFirstByBarcode(barcode)
        .map(product -> productMapping(product, generateBarcode))
        .orElseThrow(() -> new ProductNotFoundException(barcode));
  }

  private GetProduct productMapping(Product product, boolean generateBarcode) {
    GetProduct response = new GetProduct();
    response.setId(product.getId());
    response.setName(product.getName());
    response.setUom(product.getUom());
    response.setPrice(product.getPrice());
    response.setBarcode(product.getBarcode());

    if (generateBarcode) {
      response.setImgBarcode(generateEan13Image(product.getBarcode()));
    }
    return response;
  }

  /** @return PNG base64, atau null bila barcode bukan EAN-13 yang valid. */
  private String generateEan13Image(String barcode) {
    if (barcode == null || !EAN13_INPUT.matcher(barcode).matches()) {
      log.warn("Barcode '{}' bukan 12/13 digit, gambar barcode dilewati", barcode);
      return null;
    }
    try {
      EAN13Bean generator = new EAN13Bean();
      BitmapCanvasProvider canvas =
          new BitmapCanvasProvider(160, BufferedImage.TYPE_BYTE_BINARY, false, 0);
      generator.generateBarcode(canvas, barcode);
      return encodeToString(canvas.getBufferedImage(), "png");
    } catch (Exception e) {
      log.warn("Gagal membuat gambar barcode '{}': {}", barcode, e.getMessage());
      return null;
    }
  }

  private String encodeToString(BufferedImage image, String type) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      ImageIO.write(image, type, bos);
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
  }

}
