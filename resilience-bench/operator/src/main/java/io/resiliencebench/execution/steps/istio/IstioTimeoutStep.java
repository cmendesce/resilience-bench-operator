package io.resiliencebench.execution.steps.istio;

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
public class IstioTimeoutStep extends IstioExecutorStep {

  private final static Logger logger = LoggerFactory.getLogger(IstioTimeoutStep.class);

  public IstioTimeoutStep(KubernetesClient kubernetesClient, IstioClient istioClient, CustomResourceRepository<ResilientService> serviceRepository) {
    super(kubernetesClient, istioClient, serviceRepository);
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return scenario
            .getSpec()
            .getConnectors()
            .stream()
            .anyMatch(connector -> connector.getIstio() != null && connector.getIstio().getTimeout() != null);
  }

  @Override
  protected void internalExecute(Scenario scenario, ExecutionQueue queue) {
    for (var connector : scenario.getSpec().getConnectors()) {
      configureTimeoutOnDestination(scenario.getMetadata().getNamespace(), connector);
    }
  }

  private void configureTimeoutOnDestination(String namespace, Connector connector) {
    var virtualService = findVirtualService(namespace, connector.getDestination().getName());

    var http = virtualService.getSpec().getHttp().get(0);
    http.setTimeout(null);
    createTimeoutPolicy(connector.getIstio().getTimeout()).ifPresent(http::setTimeout);

    istioClient()
            .v1beta1()
            .virtualServices()
            .inNamespace(virtualService.getMetadata().getNamespace())
            .resource(virtualService)
            .update();
  }

  private Optional<String> createTimeoutPolicy(Map<String, Object> patternConfig) {
    var timeout = (Integer) patternConfig.get("timeout");
    if (timeout != null && timeout >= 0) {
      return of(timeout + "ms");
    } else {
      logger.error("Timeout not configured. Attempts and perTryTimeout are required for retry pattern configuration.");
      return empty();
    }
  }
}
