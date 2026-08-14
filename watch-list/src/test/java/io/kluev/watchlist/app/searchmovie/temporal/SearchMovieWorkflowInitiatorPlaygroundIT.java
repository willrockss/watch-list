package io.kluev.watchlist.app.searchmovie.temporal;

import io.kluev.watchlist.app.chat.ChatMessage;
import io.kluev.watchlist.infra.googlesheet.clientimpl.GoogleSheetsClient;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Disabled
@Tag("IntegrationTest")
@MockBean({GoogleSheetsClient.class, JdbcClient.class})
@SpringBootTest(
        properties = {
                "toggles.download-coordinator.enabled=false",
                "integration.telegram-bot.session-store-type=NOOP",
                "workflow-engine=TEMPORAL",
                "spring.temporal.namespace=watch-list-test",
                "spring.temporal.connection.target=192.168.0.120:7233"
        }
)
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
class SearchMovieWorkflowInitiatorPlaygroundIT {

    private static final String NAMESPACE = "watch-list-test";
    private static final String TARGET = "192.168.0.120:7233";

    @Autowired
    private SearchMovieWorkflowInitiator initiator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SearchMovieActivitiesImpl searchMovieActivities;

    @Test
    @SneakyThrows
    public void should_start_temporal_workflow_on_chat_message() {
        val msg = ChatMessage.builder()
                .source("Test")
                .id("test-msg-1")
                .username("tester")
                .chatId("12345")
                .text("Терминатор")
                .build();

        eventPublisher.publishEvent(msg);

        val wfId = "%s-%s-%s".formatted(msg.source(), msg.chatId(), msg.id());
        WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(TARGET)
                        .build());
        try {
            WorkflowClient client = WorkflowClient.newInstance(
                    stubs,
                    WorkflowClientOptions.newBuilder()
                            .setNamespace(NAMESPACE)
                            .build());
            WorkflowStub stub = client.newUntypedWorkflowStub(
                    WorkflowExecution.newBuilder().setWorkflowId(wfId).build(),
                    Optional.empty());

            // Blocks until the workflow execution completes
            stub.getResult(10, TimeUnit.MINUTES, Void.class);
            System.out.println("Temporal workflow completed: " + wfId);

            Assertions.assertEquals(
                    WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
                    stub.describe().getWorkflowExecutionInfo().getStatus());
        } finally {
            stubs.shutdown();
        }
    }
}