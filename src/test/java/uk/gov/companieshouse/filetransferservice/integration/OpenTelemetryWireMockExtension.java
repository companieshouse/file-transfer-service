package uk.gov.companieshouse.filetransferservice.integration;

import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilterV2;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

/**
 * WireMock extension to convert OpenTelemetry Protobuf requests to JSON for easier assertions and debugging.
 * This is useful when using WireMock to stub OpenTelemetry endpoints, and you want to inspect the
 * request payloads in a human-readable format.
 */
public class OpenTelemetryWireMockExtension implements StubRequestFilterV2 {

    private final JsonFormat.Printer jsonPrinter;

    public OpenTelemetryWireMockExtension() {
        jsonPrinter = JsonFormat.printer();
    }

    @Override
    public RequestFilterAction filter(Request request,
                                      ServeEvent serveEvent) {
        if (!isProtobufRequest(request)) {
            return RequestFilterAction.continueWith(request);
        }
        try {
            String requestAsJson = "";
            if (request.getUrl().contains("/v1/traces")) {
                ExportTraceServiceRequest exportTraceServiceRequest = ExportTraceServiceRequest.parseFrom(request.getBody());
                requestAsJson = jsonPrinter.print(exportTraceServiceRequest);
            }
            if (request.getUrl().contains("/v1/logs")) {
                ExportLogsServiceRequest exportLogsServiceRequest = ExportLogsServiceRequest.parseFrom(request.getBody());
                requestAsJson = jsonPrinter.print(exportLogsServiceRequest);
            }
            return RequestFilterAction.continueWith(
                    ImmutableRequest.create()
                            .withAbsoluteUrl(request.getAbsoluteUrl())
                            .withMethod(request.getMethod())
                            .withClientIp(request.getClientIp())
                            .withHeaders(request.getHeaders())
                            .withBody(requestAsJson.getBytes(StandardCharsets.UTF_8))
                            .withBrowserProxyRequest(request.isBrowserProxyRequest())
                            .withMultipart(request.isMultipart())
                            .build());

        } catch (InvalidProtocolBufferException e) {
            return RequestFilterAction.continueWith(request);
        }
    }

    private boolean isProtobufRequest(Request request) {
        return request.contentTypeHeader().containsValue("application/x-protobuf")
                || request.contentTypeHeader().containsValue("application/protobuf");
    }

    @Override
    public String getName() {
        return "OpenTelemetryWireMockExtension";
    }
}
