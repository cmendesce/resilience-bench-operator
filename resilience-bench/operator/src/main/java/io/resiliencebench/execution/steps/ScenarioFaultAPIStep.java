package io.resiliencebench.execution.steps;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static io.resiliencebench.support.Annotations.ENVOY_PORT;
import static io.resiliencebench.support.Annotations.ENVOY_SERVICE;

@Service
 public class ScenarioFaultAPIStep extends AbstractEnvironmentStep {

  private final static Logger logger = LoggerFactory.getLogger(ScenarioFaultAPIStep.class);
  private final RestTemplate restTemplate;
  
  public ScenarioFaultAPIStep(KubernetesClient kubernetesClient,
                              CustomResourceRepository<ResilientService> resilientServiceRepository,
                              RestTemplate restTemplate) {
    super(kubernetesClient, resilientServiceRepository);
    this.restTemplate = restTemplate;
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return scenario.getSpec().getFault() != null;
  }

  // Does not matter what was set in fault.provider. we'll apply it as env var of envoy container.
  @Override
  protected void internalExecute(Scenario scenario, ExecutionQueue queue) {
    var fault = scenario.getSpec().getFault();
    for (var service : fault.getServices()) {
      var resilientService = resilientServiceRepository.get(scenario.getMetadata().getNamespace(), service);
      applyServiceFault(scenario, resilientService);
    }
  }

  public void applyServiceFault(Scenario scenario, ResilientService resilientService) {
    var serviceName = resilientService.getMetadata().getAnnotations().get(ENVOY_SERVICE);

    var serviceSpec = kubernetesClient()
            .services()
            .inNamespace(resilientService.getMetadata().getNamespace())
            .withName(serviceName)
            .get().getSpec();

    var portNumber = 9901; // default admin envoy port

    for (var port : serviceSpec.getPorts()) {
      if (port.getName().equals(ENVOY_PORT)) {
        portNumber = port.getPort();
      }
    }

    var clusterIP = serviceSpec.getClusterIP();
    if (clusterIP == null || "None".equalsIgnoreCase(clusterIP)) {
      logger.error("Service {} has no ClusterIP or uses 'Headless' service.", serviceName);
      return;
    }

    try {
      var runtimeModifyUrl = "http://%s:%d/runtime_modify?filter.http.fault.abort.percent=%d".formatted(
              clusterIP, portNumber, scenario.getSpec().getFault().getPercentage()
      );
      var response = restTemplate.postForObject(runtimeModifyUrl, null, String.class);
      logger.info("Fault {}% applied for {}. Response {}",
              scenario.getSpec().getFault().getPercentage(),
              resilientService.getMetadata().getName(),
              response
      );
    } catch (RestClientException e) {
      logger.error("Service fault not applied for {}. Error {}", resilientService.getMetadata().getName(), e);
    }
  }
}
