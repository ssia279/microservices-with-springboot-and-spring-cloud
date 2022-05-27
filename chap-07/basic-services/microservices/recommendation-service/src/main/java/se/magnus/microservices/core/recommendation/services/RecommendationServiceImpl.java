package se.magnus.microservices.core.recommendation.services;

import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.http.ServiceUtil;

import java.util.logging.Level;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final RecommendationRepository repository;

  private final RecommendationMapper mapper;
  private ServiceUtil serviceUtil;


  @Autowired
  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }


  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }

    RecommendationEntity entity = this.mapper.apiToEntity(body);
    Mono<Recommendation> newEntity = this.repository.save(entity)
        .log(LOG.getName(), Level.FINE)
        .onErrorMap(DuplicateKeyException.class,
            ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id: " + body.getRecommendationId()))
        .map(recommendationEntity -> this.mapper.entityToApi(recommendationEntity));

    return newEntity;
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get recommendations for product with id={}", productId);

    return this.repository.findByProductId(productId)
        .log(LOG.getName(), Level.FINE)
        .map(entity -> this.mapper.entityToApi(entity))
        .map(recommendation -> setServiceAddress(recommendation));
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    return this.repository.deleteAll(this.repository.findByProductId(productId));
  }

  private Recommendation setServiceAddress(Recommendation recommendation) {
    recommendation.setServiceAddress(this.serviceUtil.getServiceAddress());
    return recommendation;
  }
}
