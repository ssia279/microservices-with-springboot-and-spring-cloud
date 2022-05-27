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
import reactor.test.StepVerifier;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.ASC;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(this.repository.deleteAll()).verifyComplete();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(this.repository.save(entity))
            .expectNextMatches(createdEntity -> {
              this.savedEntity = createdEntity;
              return areProductEqual(entity, this.savedEntity);
            }).verifyComplete();
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(this.repository.save(newEntity))
        .expectNextMatches(createdEntity ->
          newEntity.getProductId() == createdEntity.getProductId())
        .verifyComplete();

    StepVerifier.create(this.repository.findById(newEntity.getId()))
        .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
        .verifyComplete();

    StepVerifier.create(this.repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {
    this.savedEntity.setName("n2");

    StepVerifier.create(this.repository.save(this.savedEntity))
        .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
        .verifyComplete();

    StepVerifier.create(this.repository.findById(this.savedEntity.getId()))
        .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 && foundEntity.getName().equals("n2"))
        .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(this.repository.delete(this.savedEntity))
        .verifyComplete();
    StepVerifier.create(this.repository.existsById(this.savedEntity.getId()))
        .expectNext(false).verifyComplete();
  }

  @Test
  void getProductById(){
    StepVerifier.create(this.repository.findByProductId(this.savedEntity.getProductId()))
        .expectNextMatches(foundEntity -> areProductEqual(this.savedEntity, foundEntity))
        .verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(this.savedEntity.getProductId(), "n", 1);
    StepVerifier.create(this.repository.save(entity))
        .expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {
    ProductEntity entity1 = this.repository.findById(this.savedEntity.getId()).block();
    ProductEntity entity2 = this.repository.findById(this.savedEntity.getId()).block();

    entity1.setName("n1");
    this.repository.save(entity1).block();

    StepVerifier.create(this.repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    StepVerifier.create(this.repository.findById(this.savedEntity.getId()))
        .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 && foundEntity.getName().equals("n1"))
        .verifyComplete();
  }
  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return (expectedEntity.getId().equals(actualEntity.getId()))
        && (expectedEntity.getVersion() == actualEntity.getVersion())
        && (expectedEntity.getProductId() == actualEntity.getProductId())
        && (expectedEntity.getName().equals(actualEntity.getName()))
        && (expectedEntity.getWeight() == actualEntity.getWeight());
  }

}
