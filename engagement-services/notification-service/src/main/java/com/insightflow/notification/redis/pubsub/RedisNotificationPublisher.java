package com.insightflow.notification.redis.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic notificationRealtimeTopic;

    public void publish(RedisRealtimeEvent event) {
        if (event == null) {
            return;
        }
        redisTemplate.convertAndSend(notificationRealtimeTopic.getTopic(), event);
        log.info("Redis realtime published type={} recipientId={} channel={}",
                event.getType(),
                event.getRecipientId(),
                notificationRealtimeTopic.getTopic());
    }
}
