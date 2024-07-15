package io.resiliencebench.execution.steps.istio;

import io.fabric8.istio.api.networking.v1beta1.HTTPFaultInjection;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Connector;
import io.resiliencebench.resources.scenario.Fault;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IstioFaultStep extends IstioExecutorStep<Scenario> {

  private final static Logger logger = LoggerFactory.getLogger(IstioFaultStep.class);

  public IstioFaultStep(KubernetesClient kubernetesClient, IstioClient istioClient, CustomResourceRepository<ResilientService> serviceRepository) {
    super(kubernetesClient, istioClient, serviceRepository);
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return false;
  }

  @Override
  public Scenario internalExecute(Scenario scenario, ExecutionQueue executionQueue) {
    for (var connector : scenario.getSpec().getConnectors()) {
      configureFaultOnTarget(scenario.getMetadata().getNamespace(), connector);
    }
    return scenario;
  }

  private void configureFaultOnTarget(String namespace, Connector connector) {
//    var targetService = findVirtualService(namespace, connector.getDestination());
//    var virtualService = targetService
//            .edit()
//            .editSpec()
//            .editFirstHttp();
//    createFault(target.getFault()).ifPresent(virtualService::withFault);
//    var editedVirtualService = virtualService.endHttp().endSpec().build();
//
//    istioClient()
//            .v1beta1()
//            .virtualServices()
//            .inNamespace(targetService.getMetadata().getNamespace())
//            .resource(editedVirtualService)
//            .update();
  }

  public Optional<HTTPFaultInjection> createFault(Fault faultTemplate) {
    if (faultTemplate == null || (faultTemplate.getAbort() == null && faultTemplate.getDelay() == null)) {
      logger.error("Fault template is null. No fault to configure.");
      return Optional.empty();
    }

    var builder = new HTTPFaultInjection().toBuilder();
    if (faultTemplate.getDelay() != null) {
      builder.withNewDelay()
              .withNewPercentage(faultTemplate.getPercentage().doubleValue())
              .withNewHTTPFaultInjectionDelayFixedHttpType(faultTemplate.getDelay().duration() + "ms")
              .endDelay();
    } else if (faultTemplate.getAbort() != null) {
      builder.withNewAbort()
              .withNewPercentage(faultTemplate.getPercentage().doubleValue())
              .withNewHTTPFaultInjectionAbortHttpStatusErrorType(faultTemplate.getAbort().httpStatus())
              .endAbort();
    }
    return Optional.of(builder.build());
  }
}
