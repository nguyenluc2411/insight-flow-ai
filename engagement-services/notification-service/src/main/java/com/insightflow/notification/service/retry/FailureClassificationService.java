package com.insightflow.notification.service.retry;

import com.insightflow.notification.enums.FailureType;

public interface FailureClassificationService {

    FailureType classify(Throwable throwable);

    boolean isRetryable(Throwable throwable);
}

