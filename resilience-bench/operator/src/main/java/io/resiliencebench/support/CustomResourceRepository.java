package io.resiliencebench.support;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getName;

public class CustomResourceRepository<T extends CustomResource> {

  private static final Logger logger = LoggerFactory.getLogger(CustomResourceRepository.class);

  private final MixedOperation<T, KubernetesResourceList<T>, Resource<T>> resourceOperation;

  public CustomResourceRepository(KubernetesClient kubernetesClient, Class<T> resourceClass) {
    this(kubernetesClient.resources(resourceClass));
  }

  public CustomResourceRepository(MixedOperation<T, KubernetesResourceList<T>, Resource<T>> resourceOperation) {
    this.resourceOperation = resourceOperation;
  }

  private NonNamespaceOperation<T, KubernetesResourceList<T>, Resource<T>> inNamespace(T resource) {
    return this.resourceOperation.inNamespace(resource.getMetadata().getNamespace());
  }

  public T create(T resource) {
    return inNamespace(resource).resource(resource).create();
  }

  public T update(T resource) {
    logger.debug(
            "Trying to replace resource {}, version: {}",
            getName(resource),
            resource.getMetadata().getResourceVersion());
    return resource(resource).update();
  }

  public T updateStatus(T resource) {
    logger.trace("Updating status for resource: {}", resource);
    return resource(resource)
            .lockResourceVersion()
            .updateStatus();
  }

  public T patchStatus(T resource, T originalResource) {
    logger.trace("Updating status for resource: {}", resource);
    String resourceVersion = resource.getMetadata().getResourceVersion();
    // don't do optimistic locking on patch
    originalResource.getMetadata().setResourceVersion(null);
    resource.getMetadata().setResourceVersion(null);
    try {
      return resource(originalResource)
              .editStatus(r -> resource);
    } finally {
      // restore initial resource version
      originalResource.getMetadata().setResourceVersion(resourceVersion);
      resource.getMetadata().setResourceVersion(resourceVersion);
    }
  }

  public void deleteAll(String namespace) {
    resourceOperation.inNamespace(namespace).list().getItems().forEach(r -> {
      logger.debug("Deleting resource: {}", r);
      inNamespace(r).delete();
    });
  }

  public Optional<T> find(ObjectMeta meta) {
    return this.find(meta.getNamespace(), meta.getName());
  }

  public T get(String namespace, String name) {
    return find(namespace, name)
            .orElseThrow(() -> new RuntimeException(String.format("Resource %s.%s not found", namespace, name)));
  }

  public Optional<T> find(String namespace, String name) {
    if (namespace != null) {
      return Optional.ofNullable(resourceOperation.inNamespace(namespace).withName(name).get());
    } else {
      return Optional.ofNullable(resourceOperation.withName(name).get());
    }
  }

  public List<T> list(String namespace) {
    return this.resourceOperation.inNamespace(namespace).list().getItems();
  }

  private Resource<T> resource(T resource) {
    return resource instanceof Namespaced ? resourceOperation
            .inNamespace(resource.getMetadata().getNamespace())
            .resource(resource) : resourceOperation.resource(resource);
  }
}
