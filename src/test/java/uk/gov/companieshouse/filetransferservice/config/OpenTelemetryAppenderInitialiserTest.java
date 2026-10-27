package uk.gov.companieshouse.filetransferservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OpenTelemetryAppenderInitialiserTest {

    @Test
    void shouldInstallAppenderGivenEnabledProperty() {
        getContextRunnerWithNoOpOpenTelemetry()
                .withPropertyValues("management.opentelemetry.enabled:true")
                .run(context ->
                        assertThat(context).hasSingleBean(OpenTelemetryAppenderInitialiser.class));
    }

    @Test
    void shouldNotContainInitialiserGivenDisabledProperty() {
        getContextRunnerWithNoOpOpenTelemetry()
                .withPropertyValues("management.opentelemetry.enabled:false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(OpenTelemetryAppenderInitialiser.class));
    }

    private static ApplicationContextRunner getContextRunnerWithNoOpOpenTelemetry() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OpenTelemetryAppenderInitialiser openTelemetryAppenderInitialiser = new OpenTelemetryAppenderInitialiser(openTelemetry);
        return new ApplicationContextRunner()
                .withBean(
                        OpenTelemetryAppenderInitialiser.class,
                        () -> openTelemetryAppenderInitialiser);
    }
}
