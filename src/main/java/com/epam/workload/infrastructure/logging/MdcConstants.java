package com.epam.workload.infrastructure.logging;

public final class MdcConstants {

    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    public static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    private MdcConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
