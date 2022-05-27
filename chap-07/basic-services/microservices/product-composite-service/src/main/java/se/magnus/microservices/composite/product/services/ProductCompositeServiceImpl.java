package se.magnus.microservices.composite.product.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);
  private final ServiceUtil serviceUtil;
  private ProductCompositeIntegration integration;

  @Autowired
  public ProductCompositeServiceImpl(
      ServiceUtil serviceUtil,
      ProductCompositeIntegration integration) {

    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public Mono<ProductAggregate> getProduct(int productId) {

    LOG.info("Will get composite product info for product.id={}", productId);
    // call 3 APIs in parallel, uses static zip method on Mono class.
    // zip method will run parallel requests and zip the results together once they are complete.

    return Mono.zip(
      aggregateInfo -> createProductAggregate(
          (Product) aggregateInfo[0],
          (List<Recommendation>) aggregateInfo[1],
          (List<Review>) aggregateInfo[2],
          this.serviceUtil.getServiceAddress()),
            this.integration.getProduct(productId),
        this.integration.getRecommendations(productId).collectList(),
        this.integration.getReviews(productId).collectList())
        .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
        .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {
    try {
      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      this.integration.createProduct(product);

      if (body.getRecommendations() != null) {
        body.getRecommendations().forEach(r -> {
          Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(),
              r.getAuthor(), r.getRate(), r.getContent(), null);
          this.integration.createRecommendation(recommendation);
        });
      }

      if (body.getReviews() != null) {
        body.getReviews().forEach(r -> {
          Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(),
              r.getContent(), null);
          this.integration.createReview(review);
        });
      }
    }
    catch(RuntimeException ex) {
      LOG.debug("createCompositeProduct failed", ex);
      throw  ex;
    }
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    this.integration.deleteProduct(productId);
    this.integration.deleteRecommendations(productId);
    this.integration.deleteReviews(productId);
  }

  private ProductAggregate createProductAggregate(
      Product product,
      List<Recommendation> recommendations,
      List<Review> reviews,
      String serviceAddress) {

    int productId = product.getProductId();;
    String name = product.getName();
    int weight = product.getWeight();

    List<RecommendationSummary> recommendationSummaries =
        (recommendations == null) ? null : recommendations.stream()
            .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
            .collect(Collectors.toList());

    List<ReviewSummary> reviewSummaries =
        (reviews == null) ? null : reviews.stream()
            .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
            .collect(Collectors.toList());

    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);

  }
}
