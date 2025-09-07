package io.resiliencebench.support;

/**
 * Represents a reference to a ConfigMap object in Kubernetes.
 */
public class ConfigMapReference {
  private String name;
  private String file;

  public ConfigMapReference() {
  }

  public ConfigMapReference(String name, String file) {
    this.name = name;
    this.file = file;
  }

  public String getName() {
    return name;
  }

  public String getFile() {
    return file;
  }
}
