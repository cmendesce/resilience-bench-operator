package io.resiliencebench.execution.steps.istio;

import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettings;
import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettingsHTTPSettings;
import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.OutlierDetection;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Connector;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.springframework.stereotype.Service;

import static io.resiliencebench.support.Annotations.DESTINATION_RULE;
import static java.lang.String.format;

@Service
public class IstioCircuitBreakerStep extends IstioExecutorStep {
  public IstioCircuitBreakerStep(KubernetesClient kubernetesClient, IstioClient istioClient, CustomResourceRepository<ResilientService> serviceRepository) {
    super(kubernetesClient, istioClient, serviceRepository);
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return scenario
            .getSpec()
            .getConnectors()
            .stream()
            .anyMatch(connector -> connector.getIstio() != null && connector.getIstio().getCircuitBreaker() != null);
  }

  @Override
  protected void internalExecute(Scenario scenario, ExecutionQueue queue) {
    for (var connector : scenario.getSpec().getConnectors()) {
      configureCircuitBreakerOnDestination(scenario.getMetadata().getNamespace(), connector);
    }
  }

  public void configureCircuitBreakerOnDestination(String namespace, Connector connector) {
    var destinationRule = findDestinationRule(namespace, connector.getDestination().getName());
    var trafficPolicy = destinationRule.getSpec().getTrafficPolicy();

    trafficPolicy.setConnectionPool(null);
    trafficPolicy.setOutlierDetection(null);

    var cbConfig = connector.getIstio().getCircuitBreaker();

    var connectionPool = new ConnectionPoolSettingsHTTPSettings();
    connectionPool.setHttp1MaxPendingRequests((Integer) cbConfig.get("http1MaxPendingRequests"));

    var outlierDetection = new OutlierDetection();
    outlierDetection.setConsecutive5xxErrors((Integer) cbConfig.get("consecutive5xxErrors"));
    outlierDetection.setInterval(cbConfig.get("interval").toString());
    outlierDetection.setBaseEjectionTime(cbConfig.get("baseEjectionTime").toString());
    outlierDetection.setMaxEjectionPercent((Integer) cbConfig.get("maxEjectionPercent"));


    trafficPolicy.setConnectionPool(new ConnectionPoolSettings(connectionPool, null));

    /**
     *
     * connectionPool:
     *       tcp:
     *         maxConnections: 1
     *       http:
     *         http1MaxPendingRequests: 1
     *         maxRequestsPerConnection: 1
     *     outlierDetection:
     *       consecutive5xxErrors: 1
     *       interval: 1s
     *       baseEjectionTime: 3m
     *       maxEjectionPercent: 100
     */

//    var  createCircuitBreakerPolicy(connector.getIstio().getCircuitBreaker());
//
    istioClient()
            .v1beta1()
            .destinationRules()
            .inNamespace(destinationRule.getMetadata().getNamespace())
            .resource(destinationRule)
            .update();
  }


  public DestinationRule findDestinationRule(String namespace, String name) {
    var targetService = getServiceRepository().find(namespace, name);

    if (targetService.isPresent()) {
      var destinationRouteName = targetService.get().getMetadata().getAnnotations().get(DESTINATION_RULE);
      return istioClient()
              .v1beta1()
              .destinationRules()
              .inNamespace(namespace)
              .withName(destinationRouteName)
              .get();
    } else {
      throw new RuntimeException(format("Service not found: %s.%s", namespace, name));
    }
  }
}
