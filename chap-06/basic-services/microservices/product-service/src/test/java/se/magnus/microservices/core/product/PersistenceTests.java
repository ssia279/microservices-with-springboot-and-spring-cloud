package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.ASC;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class PersistenceTests extends MongoDbTestBase{

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    this.repository.deleteAll();;

    ProductEntity entity = new ProductEntity(1, "n", 1);
    savedEntity = this.repository.save(entity);

    assertEqualsProduct(entity, savedEntity);
  }

  @Test
  void create() {

    ProductEntity newEntity = new ProductEntity(2, "n", 2);
    this.repository.save(newEntity);

    ProductEntity foundEntity = this.repository.findById(newEntity.getId()).get();
    assertEqualsProduct(newEntity, foundEntity);

    assertEquals(2, this.repository.count());
  }

  @Test
  void update() {
    this.savedEntity.setName("n2");
    this.repository.save(this.savedEntity);

    ProductEntity foundEntity = this.repository.findById(this.savedEntity.getId()).get();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("n2", foundEntity.getName());
  }

  @Test
  void delete() {
    this.repository.delete(this.savedEntity);
    assertFalse(this.repository.existsById(this.savedEntity.getId()));
  }

  @Test
  void getByProdcutId() {
    Optional<ProductEntity> entity = this.repository.findByProductId(this.savedEntity.getProductId());

    assertTrue(entity.isPresent());
    assertEqualsProduct(this.savedEntity, entity.get());
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      ProductEntity entity = new ProductEntity(this.savedEntity.getProductId(), this.savedEntity.getName(), this.savedEntity.getWeight());
      this.repository.save(entity);
    });
  }

  @Test
  void optimistLockError() {
    ProductEntity entity1 = this.repository.findById(this.savedEntity.getId()).get();
    ProductEntity entity2 = this.repository.findById(this.savedEntity.getId()).get();

    entity1.setName("n1");
    this.repository.save(entity1);
    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setName("n2");
      this.repository.save(entity2);
    });

    ProductEntity updatedEntity = this.repository.findById(this.savedEntity.getId()).get();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("n1", updatedEntity.getName());
  }

  private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
    assertEquals(expectedEntity.getId(), actualEntity.getId());
    assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
    assertEquals(expectedEntity.getName(), actualEntity.getName());
    assertEquals(expectedEntity.getWeight(), actualEntity.getWeight());
  }

  @Test
  void paging() {

    repository.deleteAll();

    List<ProductEntity> newProducts = rangeClosed(1001, 1010)
        .mapToObj(i -> new ProductEntity(i, "name " + i, i))
        .collect(Collectors.toList());
    repository.saveAll(newProducts);

    Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
    nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
    nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
    nextPage = testNextPage(nextPage, "[1009, 1010]", false);
  }

  private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
    Page<ProductEntity> productPage = repository.findAll(nextPage);
    assertEquals(expectedProductIds, productPage.getContent().stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
    assertEquals(expectsNextPage, productPage.hasNext());
    return productPage.nextPageable();
  }

}
