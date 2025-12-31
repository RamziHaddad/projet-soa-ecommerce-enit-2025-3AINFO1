package com.recommendation.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "otel.enabled", havingValue = "true")
public class TracingConfig {
    @Bean
    public OpenTelemetry openTelemetry(@Value("${otel.exporter.otlp.endpoint:http://localhost:4317}") String otlp) {
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder().setEndpoint(otlp).build();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .setResource(Resource.getDefault())
                .build();
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry otel) {
        io.opentelemetry.api.trace.Tracer oTracer = otel.getTracer("recommendation-service");
        return new OtelTracer(oTracer, null, null);
    }
}
