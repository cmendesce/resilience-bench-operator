package io.resiliencebench;

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, value = ControllerConfiguration.class)
        })
public class ResilienceBenchOperator {
  public static void main(String[] args) {
    SpringApplication.run(ResilienceBenchOperator.class, args);
  }
}
