package se.magnus.microservices.core.recommendation;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity;

  @BeforeEach
  void setupDb() {
    this.repository.deleteAll().block();

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
    this.savedEntity = this.repository.save(entity).block();

    assertEqualsRecommendation(entity, this.savedEntity);
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");

    StepVerifier.create(this.repository.save(newEntity))
        .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
        .verifyComplete();

    StepVerifier.create(this.repository.findById(newEntity.getId()))
        .expectNextMatches(foundEntity -> areRecommentationEqual(newEntity, foundEntity))
        .verifyComplete();

    StepVerifier.create(this.repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {

    this.savedEntity.setAuthor("b");
    StepVerifier.create(this.repository.save(this.savedEntity))
        .expectNextMatches(updatedEntity -> updatedEntity.getAuthor().equals("b"))
        .verifyComplete();

    StepVerifier.create(this.repository.findById(this.savedEntity.getId()))
        .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 && foundEntity.getAuthor().equals("b"))
        .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(this.repository.delete(this.savedEntity)).verifyComplete();

    StepVerifier.create(this.repository.existsById(this.savedEntity.getId())).expectNext(false).verifyComplete();
  }

  @Test
  void getByProductId() {
    StepVerifier.create(this.repository.findByProductId(this.savedEntity.getProductId()))
        .expectNextMatches(foundEntity -> areRecommentationEqual(this.savedEntity, foundEntity))
        .verifyComplete();
  }

  @Test
  void duplicateError() {

    RecommendationEntity entity = new RecommendationEntity(this.savedEntity.getProductId(), this.savedEntity.getRecommendationId(),
        this.savedEntity.getAuthor(), this.savedEntity.getRating(), this.savedEntity.getContent());

    StepVerifier.create(this.repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {
    RecommendationEntity entity1 = this.repository.findById(this.savedEntity.getId()).block();
    RecommendationEntity entity2 = this.repository.findById(this.savedEntity.getId()).block();

    entity1.setAuthor("b1");
    this.repository.save(entity1).block();

    StepVerifier.create(this.repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    StepVerifier.create(this.repository.findById(this.savedEntity.getId()))
        .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 && foundEntity.getAuthor().equals("b1"))
        .verifyComplete();
  }

  private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    assertEquals(expectedEntity.getId(), actualEntity.getId());
    assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
    assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
    assertEquals(expectedEntity.getAuthor(), actualEntity.getAuthor());
    assertEquals(expectedEntity.getRating(), actualEntity.getRating());
    assertEquals(expectedEntity.getContent(), actualEntity.getContent());
  }

  private boolean areRecommentationEqual(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    return
        (expectedEntity.getId().equals(actualEntity.getId()))
        && (expectedEntity.getVersion() == actualEntity.getVersion())
        && (expectedEntity.getProductId() == actualEntity.getProductId())
        && (expectedEntity.getAuthor().equals(actualEntity.getAuthor()))
        && (expectedEntity.getRecommendationId() == actualEntity.getRecommendationId())
        && (expectedEntity.getContent().equals(actualEntity.getContent()))
        && (expectedEntity.getRating() == expectedEntity.getRating());
  }
}
