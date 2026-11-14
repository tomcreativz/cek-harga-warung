package com.alpha.products.AlphaProducts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.alpha.products.AlphaProducts.entity.Product;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.alpha.products.AlphaProducts.config.JpaAuditingConfig;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

  @Autowired ProductRepository repository;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    repository.save(product("1", "111111111111", "SARIMI ISI2 KOYA JERUK"));
    repository.save(product("2", "222222222222", "Sarimi Soto"));
    repository.save(product("3", "333333333333", "SARIMI DIHAPUS"));
    repository.flush();
    jdbc.update("UPDATE m_products SET deleted_at = ? WHERE id = '3'", java.sql.Timestamp.from(java.time.Instant.now()));
  }

  @Test
  void auditFieldsAreFilled() {
    Product p = repository.findFirstByBarcode("111111111111").orElseThrow();
    assertThat(p.getCreatedAt()).isNotNull();
    assertThat(p.getCreatedBy()).isEqualTo("system");
  }

  @Test
  void softDeletedRowsAreHidden() {
    assertThat(repository.findFirstByBarcode("333333333333")).isEmpty();
    var page = repository.findAllByNameIsContainingIgnoreCase("sarimi", PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void searchIsCaseInsensitiveAndPaged() {
    var page = repository.findAllByNameIsContainingIgnoreCase("SARIMI", PageRequest.of(0, 1));
    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  private Product product(String id, String barcode, String name) {
    Product p = new Product(barcode, name, BigDecimal.valueOf(3500), "PCS");
    p.setId(id);
    return p;
  }
}
