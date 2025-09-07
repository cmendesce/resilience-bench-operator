package io.resiliencebench.resources;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.resiliencebench.resources.benchmark.*;
import io.resiliencebench.resources.fault.DelayFault;
import io.resiliencebench.resources.workload.Workload;
import io.resiliencebench.resources.workload.WorkloadSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

public class ScenarioFactoryTest {

  public static ConnectorTemplate createConnector(
          String connectionName,
          List<Integer> maxAttempts,
          List<Integer> backoffLimit) {

    var pattern = new PatternTemplate(
            new IstioPatternTemplate(
                    new NameValueProperties(
                            new NameValueProperties.Attribute("maxAttempts", maxAttempts),
                            new NameValueProperties.Attribute("backoffLimit", backoffLimit)
                    ),
                    null, null
            )
    );

    var delay = new DelayFault(1000);
    var fault = new BenchmarkFaultTemplate(of(10), delay);
    return new ConnectorTemplate(connectionName, new ServiceTemplate("source"), new ServiceTemplate("destination"), fault, pattern);
  }

  public static ConnectorTemplate createConnector(String connectionName) {
    return createConnector(connectionName, of(1, 2), of(100, 200));
  }

  public static Workload createWorkload(List<Integer> users) {
    var meta = new ObjectMeta();
    meta.setName("workload");
    var workload = new Workload();
    workload.setMetadata(meta);
    workload.setSpec(new WorkloadSpec(users, null));
    return workload;
  }

  @Test
  public void should_create_executions() {
    var spec = new BenchmarkSpec("workload",
            of(new ScenarioTemplate("scenario-1", of(createConnector("connector-1"))))
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(4, scenarios.size());
  }

  @Test
  public void should_create_executions_with_multiple_workloads() {
    var spec = new BenchmarkSpec("workload",
            of(new ScenarioTemplate("scenario-1", of(createConnector("connector-1"))))
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10, 20, 30));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(12, scenarios.size());
  }

  @Test
  public void should_create_executions_with_multiple_scenarios() {
    var spec = new BenchmarkSpec("workload",
            of(
                    new ScenarioTemplate("scenario-1", of(createConnector("connector-1"))),
                    new ScenarioTemplate("scenario-2", of(createConnector("connector-2")))
            )
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(8, scenarios.size());
  }

  @Test
  public void should_create_executions_with_multiple_connectors() {
    var spec = new BenchmarkSpec("workload",
            of(new ScenarioTemplate("scenario-1", of(
                            createConnector("connector-1", of(1, 2, 3), of(100, 200)),
                            createConnector("connector-2", of(1, 2), of(100, 200, 300)))
                    )
            )
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10, 20));
    var generatedScenarios = ScenarioFactory.create(benchmark, workload);
    assertEquals(72, generatedScenarios.size());
    assertEquals("scenario-1-20vu-00036", generatedScenarios.get(71).getMetadata().getName());
  }

  @Test
  public void should_create_executions_with_multiple_scenarios_and_workloads() {
    var spec = new BenchmarkSpec("workload",
            of(
                    new ScenarioTemplate("scenario-1", of(createConnector("connector-1"))),
                    new ScenarioTemplate("scenario-2", of(createConnector("connector-2")))
            )
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10, 20, 30));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(24, scenarios.size());
  }

  @Test
  public void should_create_executions_with_multiple_workloads_and_connectors() {
    var spec = new BenchmarkSpec("workload",
            of(new ScenarioTemplate("scenario-1", of(
                            createConnector("connector-1"),
                            createConnector("connector-2"))
                    )
            )
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10, 20, 30));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(48, scenarios.size());
  }

  @Test
  public void should_create_executions_with_multiple_scenarios_workloads_and_connectors() {
    var spec = new BenchmarkSpec("workload",
            of(
                    new ScenarioTemplate("scenario-1", of(
                            createConnector("connector-1"),
                            createConnector("connector-2"))
                    ),
                    new ScenarioTemplate("scenario-2", of(
                            createConnector("connector-3"),
                            createConnector("connector-4"))
                    )
            )
    );
    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(10, 20, 30));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(96, scenarios.size());
  }

  @Test
  public void should_create_scenarios_with_global_fault_config() {
    var spec = new BenchmarkSpec("workload",
            of(
                    new ScenarioTemplate("scenario-1", of(
                            createConnector("connector-1")),
                            new ScenarioFaultTemplate("envoy", List.of(25, 50), List.of("destination"))
                    )
            )
    );

    var benchmark = new Benchmark();
    benchmark.setSpec(spec);
    var workload = createWorkload(of(50));
    var scenarios = ScenarioFactory.create(benchmark, workload);

    assertEquals(8, scenarios.size());

    for (var scenario : scenarios) {
      assertNotNull(scenario.getSpec().getFault());
      var percentage = scenario.getSpec().getFault().getPercentage();
      assertTrue(percentage == 25 || percentage == 50);
    }
  }

//  @Test
//  void loadyaml() {
//    KubernetesClient client = new DefaultKubernetesClient();
//    var resource = client.resources(Benchmark.class).load(
//            getClass().getResourceAsStream("/scenario-factory-resources/connector-with-one-environment.yaml")
//    ).get();
//
//    var workload = createWorkload(of(10));
//    var scenarios = ScenarioFactory.create(resource, workload);
//    assertEquals(8, scenarios.size());
//  }
}