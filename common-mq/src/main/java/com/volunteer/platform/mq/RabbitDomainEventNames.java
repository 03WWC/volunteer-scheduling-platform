package com.volunteer.platform.mq;

public final class RabbitDomainEventNames {

    public static final String DOMAIN_EVENT_EXCHANGE = "volunteer.domain.events";

    public static final String DEAD_LETTER_EXCHANGE = "volunteer.domain.events.dlx";

    public static final String MESSAGE_TOPIC = "message-topic";

    public static final String SCHEDULE_TOPIC = "schedule-topic";

    public static final String CHECKIN_TOPIC = "checkin-topic";

    public static final String DISPATCH_NOTICE_DEAD_LETTER_TOPIC = "message-topic.dispatch-notice.dlq";

    public static final String SCHEDULE_PUBLISHED_DEAD_LETTER_TOPIC = "schedule-topic.published.dlq";

    public static final String CHECKIN_REQUESTED_DEAD_LETTER_TOPIC = "checkin-topic.requested.dlq";

    public static final String DISPATCH_NOTICE_QUEUE = "volunteer.message.dispatch-notice";

    public static final String SCHEDULE_PUBLISHED_QUEUE = "volunteer.message.schedule-published";

    public static final String CHECKIN_REQUESTED_QUEUE = "volunteer.location.checkin-requested";

    public static final String DISPATCH_NOTICE_DEAD_LETTER_QUEUE = "volunteer.message.dispatch-notice.dlq";

    public static final String SCHEDULE_PUBLISHED_DEAD_LETTER_QUEUE = "volunteer.message.schedule-published.dlq";

    public static final String CHECKIN_REQUESTED_DEAD_LETTER_QUEUE = "volunteer.location.checkin-requested.dlq";

    public static final String DISPATCH_NOTICE_REQUESTED = "DISPATCH_NOTICE_REQUESTED";

    public static final String SCHEDULE_PUBLISHED = "SCHEDULE_PUBLISHED";

    public static final String CHECKIN_REQUESTED = "CHECKIN_REQUESTED";

    public static final String CHECKIN_COMPLETED = "CHECKIN_COMPLETED";

    public static final String SALARY_TOPIC = "salary-topic";

    private RabbitDomainEventNames() {
    }
}
