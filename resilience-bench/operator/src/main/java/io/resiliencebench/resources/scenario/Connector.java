package io.resiliencebench.resources.scenario;

import io.fabric8.generator.annotation.Nullable;

public class Connector {
  private String name;

  private Service source;

  private Service destination;

  @Nullable
  private IstioPattern istio;

  @Nullable
  private Fault fault;

  public Connector() {
  }

  public String getName() {
    return name;
  }

  public Service getSource() {
    return source;
  }

  public Service getDestination() {
    return destination;
  }

  public IstioPattern getIstio() {
    return istio;
  }

  public Fault getFault() {
    return fault;
  }

  public static class Builder {
    private String name;
    private Service source;
    private Service destination;
    private IstioPattern istio;
    private Fault fault;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder source(Service source) {
      this.source = source;
      return this;
    }

    public Builder destination(Service destination) {
      this.destination = destination;
      return this;
    }

    public Builder istio(IstioPattern istio) {
      this.istio = istio;
      return this;
    }

    public Builder fault(Fault fault) {
      this.fault = fault;
      return this;
    }

    public Connector build() {
      Connector connector = new Connector();
      connector.name = name;
      connector.source = source;
      connector.destination = destination;
      connector.istio = istio;
      connector.fault = fault;
      return connector;
    }
  }
}
