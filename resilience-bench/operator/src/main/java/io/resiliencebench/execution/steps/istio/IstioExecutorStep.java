package io.resiliencebench.execution.steps.istio;

import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.execution.steps.ExecutorStep;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.springframework.stereotype.Service;

import static io.resiliencebench.support.Annotations.VIRTUAL_SERVICE;
import static java.lang.String.format;

@Service
public abstract class IstioExecutorStep extends ExecutorStep {

  private final IstioClient istioClient;
  private final CustomResourceRepository<ResilientService> serviceRepository;

  public IstioExecutorStep(
          KubernetesClient kubernetesClient,
          IstioClient istioClient,
          CustomResourceRepository<ResilientService> serviceRepository) {
    super(kubernetesClient);
    this.istioClient = istioClient;
    this.serviceRepository = serviceRepository;
  }

  public CustomResourceRepository<ResilientService> getServiceRepository() {
    return serviceRepository;
  }

  protected IstioClient istioClient() {
    return istioClient;
  }

  public VirtualService findVirtualService(String namespace, String name) {
    var targetService = getServiceRepository().find(namespace, name);

    if (targetService.isPresent()) {
      var virtualServiceName = targetService.get().getMetadata().getAnnotations().get(VIRTUAL_SERVICE);
      return istioClient
              .v1beta1()
              .virtualServices()
              .inNamespace(namespace)
              .withName(virtualServiceName)
              .get();
    } else {
      throw new RuntimeException(format("Service not found: %s.%s", namespace, name));
    }
  }
}
