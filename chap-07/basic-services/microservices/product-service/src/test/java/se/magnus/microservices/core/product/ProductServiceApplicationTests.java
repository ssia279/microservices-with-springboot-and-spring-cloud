package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase{

	@Autowired
	private WebTestClient client;

	@Autowired
	private ProductRepository repository;

	@Autowired
	@Qualifier("messageProcessor")
	private Consumer<Event<Integer, Product>> messageProcessor;

	@BeforeEach
	void setupDb() {
		this.repository.deleteAll().block();
	}

	@Test
	void getProductById() {
		int productId = 1;

		assertNull(this.repository.findByProductId(productId).block());
		assertEquals(0, (long)this.repository.count().block());

		sendCreateProductEvent(productId);

		assertNotNull(this.repository.findByProductId(productId).block());
		assertEquals(1, (long) this.repository.count().block());

		getAndVerifyProduct(productId, HttpStatus.OK)
				.jsonPath("$.productId").isEqualTo(productId);
	}

	@Test
	void contextLoads() {
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return getAndVerifyProduct("/" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
		return client.get()
				.uri("/product" + productIdPath)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private void sendCreateProductEvent(int productId) {
		Product product = new Product(productId, "Name " + productId, productId, "SA");
		Event<Integer, Product> event = new Event(Event.Type.CREATE, productId, product);
		this.messageProcessor.accept(event);
	}

}
