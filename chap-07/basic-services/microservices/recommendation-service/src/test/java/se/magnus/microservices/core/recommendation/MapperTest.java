package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.services.RecommendationMapper;

import java.util.Collections;
import java.util.List;

public class MapperTest {

  private RecommendationMapper mapper = Mappers.getMapper(RecommendationMapper.class);

  @Test
  void mapperTests() {

    assertNotNull(this.mapper);

    Recommendation api = new Recommendation(1, 2, "a", 4, "C", "adr");

    RecommendationEntity entity = this.mapper.apiToEntity(api);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getRecommendationId(), entity.getRecommendationId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getRate(), entity.getRating());
    assertEquals(api.getContent(), entity.getContent());

    Recommendation api2 = this.mapper.entityToApi(entity);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getRecommendationId(), api2.getRecommendationId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getRate(), api2.getRate());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }

  @Test
  void mapperListTests() {

    assertNotNull(this.mapper);

    Recommendation api = new Recommendation(1, 2, "a", 4, "C", "adr");
    List<Recommendation> apiList = Collections.singletonList(api);

    List<RecommendationEntity> entityList = this.mapper.apiListToEntityList(apiList);
    assertEquals(apiList.size(), entityList.size());

    RecommendationEntity entity = entityList.get(0);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getRecommendationId(), entity.getRecommendationId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getRate(), entity.getRating());
    assertEquals(api.getContent(), entity.getContent());

    List<Recommendation> api2List = this.mapper.entityListToApiList(entityList);
    assertEquals(apiList.size(), api2List.size());

    Recommendation api2 = api2List.get(0);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getRecommendationId(), api2.getRecommendationId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getRate(), api2.getRate());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }
}
