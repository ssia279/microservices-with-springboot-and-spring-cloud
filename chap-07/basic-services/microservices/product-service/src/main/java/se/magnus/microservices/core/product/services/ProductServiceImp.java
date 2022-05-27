package se.magnus.microservices.core.product.services;

import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;
import se.magnus.util.http.ServiceUtil;

import java.util.logging.Level;

@RestController
public class ProductServiceImp implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImp.class);
  private final ServiceUtil serviceUtil;
  private final ProductRepository repository;
  private final ProductMapper mapper;


  @Autowired
  public ProductServiceImp(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }

    ProductEntity entity = this.mapper.apiToEntity(body);
    Mono<Product> newEntity = this.repository.save(entity)
        .log(LOG.getName(), Level.FINE)
        .onErrorMap(
            DuplicateKeyException.class,
            ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
        .map(createdEntity -> this.mapper.entityToApi(createdEntity));

    return newEntity;
  }

  @Override
  public Mono<Product> getProduct(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get product info for id={}", productId);

    return this.repository.findByProductId(productId)
        .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
        .log(LOG.getName(), Level.FINE)
        .map(entity -> this.mapper.entityToApi(entity))
        .map(product -> setServiceAddress(product));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    return this.repository.findByProductId(productId).log(LOG.getName(), Level.FINE)
        .map(productEntity -> this.repository.delete(productEntity)).flatMap(e -> e);
  }

  private Product setServiceAddress(Product product) {
    product.setServiceAddress(this.serviceUtil.getServiceAddress());

    return product;
  }
}
