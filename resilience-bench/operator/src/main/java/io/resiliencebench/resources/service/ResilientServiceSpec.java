package io.resiliencebench.resources.service;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LabelSelector;

import java.util.List;

public class ResilientServiceSpec {

  private LabelSelector selector;
  private List<EnvVar> envs;
  private String appContainerName;
  private String faultContainerName;

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public List<EnvVar> getEnvs() {
    return envs;
  }

  public void setEnvs(List<EnvVar> envs) {
    this.envs = envs;
  }

  public String getAppContainerName() {
    return appContainerName;
  }

  public void setAppContainerName(String appContainerName) {
    this.appContainerName = appContainerName;
  }

  public String getFaultContainerName() {
    return faultContainerName;
  }

  public void setFaultContainerName(String faultContainerName) {
    this.faultContainerName = faultContainerName;
  }
}
