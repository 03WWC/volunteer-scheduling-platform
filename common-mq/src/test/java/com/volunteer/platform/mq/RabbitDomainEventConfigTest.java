package com.volunteer.platform.mq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitDomainEventConfigTest {

    @Test
    void dispatchNoticeQueueRoutesFailedMessagesToDeadLetterExchange() {
        RabbitDomainEventConfig config = new RabbitDomainEventConfig();

        Queue queue = config.dispatchNoticeQueue();

        assertThat(queue.getName()).isEqualTo(RabbitDomainEventNames.DISPATCH_NOTICE_QUEUE);
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange",
            RabbitDomainEventNames.DEAD_LETTER_EXCHANGE);
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-routing-key",
            RabbitDomainEventNames.DISPATCH_NOTICE_DEAD_LETTER_TOPIC);
    }

    @Test
    void checkinRequestedQueueRoutesFailedMessagesToDeadLetterExchange() {
        RabbitDomainEventConfig config = new RabbitDomainEventConfig();

        Queue queue = config.checkinRequestedQueue();

        assertThat(queue.getName()).isEqualTo(RabbitDomainEventNames.CHECKIN_REQUESTED_QUEUE);
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange",
            RabbitDomainEventNames.DEAD_LETTER_EXCHANGE);
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-routing-key",
            RabbitDomainEventNames.CHECKIN_REQUESTED_DEAD_LETTER_TOPIC);
    }
}
