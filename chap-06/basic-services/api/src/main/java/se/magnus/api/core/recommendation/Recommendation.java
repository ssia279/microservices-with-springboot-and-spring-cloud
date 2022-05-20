package se.magnus.api.core.recommendation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Recommendation {
  private int productId;
  private int recommendationId;
  private String author;
  private int rate;
  private String content;
  private String serviceAddress;

  public Recommendation() {
    this.productId = 0;
    this.recommendationId = 0;
    this.author = null;
    this.rate = 0;
    this.content = null;
    this.serviceAddress = null;
  }

  public Recommendation(int productId, int recommendationId, String author, int rate, String content, String serviceAddress) {
    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rate = rate;
    this.content = content;
    this.serviceAddress = serviceAddress;
  }
}
