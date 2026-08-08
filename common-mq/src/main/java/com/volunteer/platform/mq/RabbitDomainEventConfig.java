package com.volunteer.platform.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(DirectExchange.class)
public class RabbitDomainEventConfig {

    @Bean
    public DirectExchange domainEventExchange() {
        return new DirectExchange(RabbitDomainEventNames.DOMAIN_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitDomainEventNames.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue dispatchNoticeQueue() {
        return QueueBuilder.durable(RabbitDomainEventNames.DISPATCH_NOTICE_QUEUE)
            .deadLetterExchange(RabbitDomainEventNames.DEAD_LETTER_EXCHANGE)
            .deadLetterRoutingKey(RabbitDomainEventNames.DISPATCH_NOTICE_DEAD_LETTER_TOPIC)
            .build();
    }

    @Bean
    public Queue dispatchNoticeDeadLetterQueue() {
        return new Queue(RabbitDomainEventNames.DISPATCH_NOTICE_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Queue schedulePublishedQueue() {
        return QueueBuilder.durable(RabbitDomainEventNames.SCHEDULE_PUBLISHED_QUEUE)
            .deadLetterExchange(RabbitDomainEventNames.DEAD_LETTER_EXCHANGE)
            .deadLetterRoutingKey(RabbitDomainEventNames.SCHEDULE_PUBLISHED_DEAD_LETTER_TOPIC)
            .build();
    }

    @Bean
    public Queue checkinRequestedQueue() {
        return QueueBuilder.durable(RabbitDomainEventNames.CHECKIN_REQUESTED_QUEUE)
            .deadLetterExchange(RabbitDomainEventNames.DEAD_LETTER_EXCHANGE)
            .deadLetterRoutingKey(RabbitDomainEventNames.CHECKIN_REQUESTED_DEAD_LETTER_TOPIC)
            .build();
    }

    @Bean
    public Queue schedulePublishedDeadLetterQueue() {
        return new Queue(RabbitDomainEventNames.SCHEDULE_PUBLISHED_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Queue checkinRequestedDeadLetterQueue() {
        return new Queue(RabbitDomainEventNames.CHECKIN_REQUESTED_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding dispatchNoticeBinding(@Qualifier("domainEventExchange") DirectExchange domainEventExchange,
                                         @Qualifier("dispatchNoticeQueue") Queue dispatchNoticeQueue) {
        return BindingBuilder.bind(dispatchNoticeQueue)
            .to(domainEventExchange)
            .with(RabbitDomainEventNames.MESSAGE_TOPIC);
    }

    @Bean
    public Binding dispatchNoticeDeadLetterBinding(@Qualifier("deadLetterExchange") DirectExchange deadLetterExchange,
                                                  @Qualifier("dispatchNoticeDeadLetterQueue")
                                                  Queue dispatchNoticeDeadLetterQueue) {
        return BindingBuilder.bind(dispatchNoticeDeadLetterQueue)
            .to(deadLetterExchange)
            .with(RabbitDomainEventNames.DISPATCH_NOTICE_DEAD_LETTER_TOPIC);
    }

    @Bean
    public Binding schedulePublishedBinding(@Qualifier("domainEventExchange") DirectExchange domainEventExchange,
                                            @Qualifier("schedulePublishedQueue") Queue schedulePublishedQueue) {
        return BindingBuilder.bind(schedulePublishedQueue)
            .to(domainEventExchange)
            .with(RabbitDomainEventNames.SCHEDULE_TOPIC);
    }

    @Bean
    public Binding checkinRequestedBinding(@Qualifier("domainEventExchange") DirectExchange domainEventExchange,
                                           @Qualifier("checkinRequestedQueue") Queue checkinRequestedQueue) {
        return BindingBuilder.bind(checkinRequestedQueue)
            .to(domainEventExchange)
            .with(RabbitDomainEventNames.CHECKIN_TOPIC);
    }

    @Bean
    public Binding schedulePublishedDeadLetterBinding(@Qualifier("deadLetterExchange")
                                                     DirectExchange deadLetterExchange,
                                                     @Qualifier("schedulePublishedDeadLetterQueue")
                                                     Queue schedulePublishedDeadLetterQueue) {
        return BindingBuilder.bind(schedulePublishedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(RabbitDomainEventNames.SCHEDULE_PUBLISHED_DEAD_LETTER_TOPIC);
    }

    @Bean
    public Binding checkinRequestedDeadLetterBinding(@Qualifier("deadLetterExchange")
                                                    DirectExchange deadLetterExchange,
                                                    @Qualifier("checkinRequestedDeadLetterQueue")
                                                    Queue checkinRequestedDeadLetterQueue) {
        return BindingBuilder.bind(checkinRequestedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(RabbitDomainEventNames.CHECKIN_REQUESTED_DEAD_LETTER_TOPIC);
    }
}
