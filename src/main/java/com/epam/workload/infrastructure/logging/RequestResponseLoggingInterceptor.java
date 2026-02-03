package com.epam.workload.infrastructure.logging;

import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        String transactionId = MDC.get(MdcConstants.TRANSACTION_ID_MDC_KEY);

        log
                .info(
                    "=== Incoming Request === TransactionId: {} | Method: {} | URI: {} | Query: {} | RemoteAddr: {}",
                    transactionId,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString() != null ? request.getQueryString() : "N/A",
                    request.getRemoteAddr());

        logHeaders(request);

        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) {

        String transactionId = MDC.get(MdcConstants.TRANSACTION_ID_MDC_KEY);

        // Log response details
        if (ex != null) {
            log
                    .error(
                        "=== Request Failed === TransactionId: {} | Method: {} | URI: {} | Status: {} | Exception: {}",
                        transactionId,
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        ex.getMessage());
        }
        else {
            String logLevel = response.getStatus() >= 400 ? "ERROR" : "INFO";

            if ("ERROR".equals(logLevel)) {
                log
                        .error(
                            "=== Request Completed with Error === TransactionId: {} | Method: {} | URI: {} | Status: {}",
                            transactionId,
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus());
            }
            else {
                log
                        .info(
                            "=== Request Completed === TransactionId: {} | Method: {} | URI: {} | Status: {}",
                            transactionId,
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus());
            }
        }
    }

    private void logHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.append(headerName).append("=").append(headerValue).append(" ");
        }

        log.debug("Request Headers: {}", headers.toString().trim());
    }

}
