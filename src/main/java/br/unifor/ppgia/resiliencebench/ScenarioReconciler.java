package br.unifor.ppgia.resiliencebench;

import br.unifor.ppgia.resiliencebench.execution.queue.ExecutionQueue;
import br.unifor.ppgia.resiliencebench.execution.queue.Item;
import br.unifor.ppgia.resiliencebench.execution.scenario.Scenario;
import br.unifor.ppgia.resiliencebench.support.Annotations;
import br.unifor.ppgia.resiliencebench.support.CustomResourceRepository;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@ControllerConfiguration
public class ScenarioReconciler implements Reconciler<Scenario> {

  private final static Logger logger = LoggerFactory.getLogger(ScenarioReconciler.class);
  private CustomResourceRepository<ExecutionQueue> queueCustomResourceRepository;

  private KubernetesClient client;

  @Override
  public UpdateControl<Scenario> reconcile(Scenario scenario, Context<Scenario> context) throws Exception {
    logger.debug("Start: {}", scenario.getMetadata().getName());

    client = context.getClient();
    queueCustomResourceRepository = new CustomResourceRepository<>(client, ExecutionQueue.class);
    var benchmark = scenario.getMetadata().getAnnotations().get(Annotations.OWNED_BY);
    var executionQueue = queueCustomResourceRepository.get(scenario.getMetadata().getNamespace(), benchmark).get(); // TODO tratar erro

    // Lógica para criar ou atualizar um Scenario
    if (deveProcessarScenario(scenario, executionQueue)) {
      if (!existeJobEmExecucao(scenario.getMetadata().getNamespace())) {
        criarJobParaScenario(scenario, context.getClient());
        atualizarStatusScenario(scenario, "processing", executionQueue);
      }
    }
    logger.info("End: {}", scenario.getMetadata().getName());
    return UpdateControl.noUpdate();
  }

  private boolean deveProcessarScenario(Scenario scenario, ExecutionQueue executionQueue) {
    var items = executionQueue.getSpec().getItems();
    var scenarioName = scenario.getMetadata().getName();
    return items.stream().filter(item -> item.getScenario().equals(scenarioName)).noneMatch(Item::isFinished);
  }

  private boolean existeJobEmExecucao(String namespace) {
    var jobs = client.batch().v1().jobs().inNamespace(namespace).list();
    var deve = jobs.getItems().stream().anyMatch(job ->
            Objects.nonNull(job.getStatus().getCompletionTime()) && job.getMetadata().getAnnotations().containsKey("scenario"));
    logger.info("Deve processar: {}", deve);
    return deve;
  }

  private void criarJobParaScenario(Scenario scenario, KubernetesClient client) {
    Job job = new JobBuilder()
            .withApiVersion("batch/v1")
            .withNewMetadata()
            .withName(UUID.randomUUID().toString())
            .withNamespace(scenario.getMetadata().getNamespace())
            .addToAnnotations("scenario", scenario.getMetadata().getName())
            .endMetadata()
            .withNewSpec()
            .withBackoffLimit(4)
            .withNewTemplate()
            .withNewSpec()
            .withRestartPolicy("Never")
            .addNewContainer()
            .withName("kubectl")
            .withCommand("sleep", "10")
            .withImage("alpine")
            .endContainer()
            .endSpec()
            .endTemplate().and().build();

    job = client.batch().v1().jobs().resource(job).create();
    logger.debug("Job criada: {}", job.getMetadata().getName());
  }

  private void atualizarStatusScenario(Scenario scenario, String status, ExecutionQueue executionQueue) {
    executionQueue.getSpec().getItems().stream()
            .filter(item -> item.getScenario().equals(scenario.getMetadata().getName()))
            .findFirst()
            .ifPresent(item -> {
              executionQueue.getMetadata().setNamespace(scenario.getMetadata().getNamespace());
              item.setStatus(status);
              item.setStartedAt(LocalDateTime.now().atZone(ZoneId.of("UTC")).toString());
              queueCustomResourceRepository.update(executionQueue);
            });
  }

}
