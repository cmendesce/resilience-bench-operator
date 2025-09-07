package io.resiliencebench.config;

import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.resources.workload.Workload;
import io.resiliencebench.support.CustomResourceRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class OperatorConfiguration {

  private static final Logger log = LoggerFactory.getLogger(OperatorConfiguration.class);

  private KubernetesClient kubernetesClient;
  private IstioClient istioClient;

  @Bean
  KubernetesClient kubernetesClient() {
    kubernetesClient = new KubernetesClientBuilder().build();
    return kubernetesClient;
  }

  @Bean
  IstioClient istioClient(KubernetesClient kubernetesClient) {
    istioClient = new DefaultIstioClient(kubernetesClient);
    return istioClient;
  }

  @PreDestroy
  public void closeClients() {
    if (istioClient != null) {
      istioClient.close();
    }

    if (kubernetesClient != null) {
      kubernetesClient.close();
    }
  }

  @Bean(destroyMethod = "stop")
  @ConditionalOnMissingBean(Operator.class)
  Operator operator(KubernetesClient kubernetesClient, List<Reconciler<?>> reconcilers) {
    var operator = new Operator((overrider) -> overrider.withKubernetesClient(kubernetesClient));
    reconcilers.forEach(operator::register);
    if (!reconcilers.isEmpty()) {
      operator.start();
    } else {
      log.warn("No Reconcilers found in the application context: Not starting the Operator");
    }
    return operator;
  }

  @Bean
  CustomResourceRepository<Scenario> scenarioRepository(KubernetesClient kubernetesClient) {
    return new CustomResourceRepository<>(kubernetesClient, Scenario.class);
  }

  @Bean
  CustomResourceRepository<ExecutionQueue> executionRepository(KubernetesClient kubernetesClient) {
    return new CustomResourceRepository<>(kubernetesClient, ExecutionQueue.class);
  }

  @Bean
  CustomResourceRepository<Workload> workloadRepository(KubernetesClient kubernetesClient) {
    return new CustomResourceRepository<>(kubernetesClient, Workload.class);
  }

  @Bean
  CustomResourceRepository<Benchmark> benchmarkRepository(KubernetesClient kubernetesClient) {
    return new CustomResourceRepository<>(kubernetesClient, Benchmark.class);
  }

  @Bean
  CustomResourceRepository<ResilientService> resilientServiceRepository(KubernetesClient kubernetesClient) {
    return new CustomResourceRepository<>(kubernetesClient, ResilientService.class);
  }

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
