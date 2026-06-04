package com.insightflow.notification.service.retry;

import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.exception.BusinessException;
import com.insightflow.notification.exception.ResourceNotFoundException;
import com.insightflow.notification.exception.NotificationDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.kafka.KafkaException;
import org.springframework.mail.MailException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Service
@Slf4j
public class FailureClassificationServiceImpl implements FailureClassificationService {

    @Override
    public FailureType classify(Throwable throwable) {
        return isRetryable(throwable) ? FailureType.RETRYABLE : FailureType.NON_RETRYABLE;
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MethodArgumentNotValidException
                    || current instanceof ConstraintViolationException
                    || current instanceof HttpMessageNotReadableException
                    || current instanceof IllegalArgumentException
                    || current instanceof ResourceNotFoundException) {
                return false;
            }
            if (current instanceof NotificationDeliveryException
                    || current instanceof TransientDataAccessResourceException
                    || current instanceof CannotAcquireLockException
                    || current instanceof RedisConnectionFailureException
                    || current instanceof KafkaException
                    || current instanceof MailException
                    || current instanceof MessagingException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof IOException) {
                return true;
            }
            if (current instanceof BusinessException) {
                return false;
            }
            current = current.getCause();
        }
        return false;
    }
}

