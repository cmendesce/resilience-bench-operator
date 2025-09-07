package io.resiliencebench.execution.steps.istio;

import io.fabric8.istio.api.networking.v1beta1.HTTPRetry;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Connector;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Service
public class IstioRetryStep extends IstioExecutorStep {

  private final static Logger log = LoggerFactory.getLogger(IstioRetryStep.class);

  public IstioRetryStep(KubernetesClient kubernetesClient, IstioClient istioClient, CustomResourceRepository<ResilientService> serviceRepository) {
    super(kubernetesClient, istioClient, serviceRepository);
  }

  @Override
  public boolean isApplicable(Scenario scenario) {
    return scenario
            .getSpec()
            .getConnectors()
            .stream()
            .anyMatch(connector -> connector.getIstio() != null && connector.getIstio().getRetry() != null);
  }

  @Override
  protected void internalExecute(Scenario scenario, ExecutionQueue executionQueue) {
    for (var connector : scenario.getSpec().getConnectors()) {
      configureRetryOnDestination(scenario.getMetadata().getNamespace(), connector);
    }
  }

  private void configureRetryOnDestination(String namespace, Connector connector) {
    var virtualService = findVirtualService(namespace, connector.getDestination().getName());

    var http = virtualService.getSpec().getHttp().get(0);
    http.setRetries(null);
    createRetryPolicy(connector.getIstio().getRetry()).ifPresent(http::setRetries);

    istioClient()
            .v1beta1()
            .virtualServices()
            .inNamespace(virtualService.getMetadata().getNamespace())
            .resource(virtualService)
            .update();
  }

  public Optional<HTTPRetry> createRetryPolicy(Map<String, Object> patternConfig) {
    var httpRetry = new HTTPRetry();
    var attempts = (Integer) patternConfig.get("attempts");
    var perTryTimeout = (Integer) patternConfig.get("perTryTimeout");
    if (attempts != null && attempts >= 0) {
      httpRetry.setAttempts(attempts);
      if (perTryTimeout != null && perTryTimeout > 0) {
        httpRetry.setPerTryTimeout(perTryTimeout + "ms");
      } else {
        log.warn("perTryTimeout must be greater than or equal to 0.");
      }
      return of(httpRetry);
    } else {
      log.error("Retry not configured. Attempts and perTryTimeout are required for retry pattern configuration.");
      return empty();
    }
  }
}
