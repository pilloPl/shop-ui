package io.pillopl.eventsource.shop.ui.integration.readmodel

import io.pillopl.eventsource.shop.ui.events.ItemOrdered
import io.pillopl.eventsource.shop.ui.events.ItemPaid
import io.pillopl.eventsource.shop.ui.events.ItemPaymentTimeout
import io.pillopl.eventsource.shop.ui.integration.IntegrationSpec
import io.pillopl.eventsource.shop.ui.readmodel.JdbcReadModel
import io.pillopl.eventsource.shop.ui.readmodel.ShopItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.messaging.support.GenericMessage
import spock.lang.Subject

import java.time.Instant

import static java.time.Instant.parse

class E2ESpec extends IntegrationSpec {

    private static final Instant ANY_TIME = parse("1995-10-23T10:12:35Z")
    private static final Instant ANY_TIME_LATER = parse("1995-10-23T10:12:35Z").plusSeconds(3600)

    private static final Instant ANY_OTHER_TIME = ANY_TIME.plusSeconds(100)
    private static final Instant YET_ANOTHER_TIME = ANY_OTHER_TIME.plusSeconds(100)

    @Subject @Autowired JdbcReadModel readModel

    @Autowired Sink sink

    def 'should store new ordered item'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.uuid == itemUUID.toString()
            item.status == 'ORDERED'
            item.when_ordered.toInstant() == ANY_TIME
            item.when_paid == null
            item.when_payment_timeout.toInstant() == ANY_TIME_LATER
            item.when_payment_marked_as_missing == null
    }

    def 'ordering should be idempotent on read side'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemIsOrdered(itemUUID, ANY_OTHER_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.when_ordered.toInstant() == ANY_TIME
    }

    def 'should update item as paid'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemIsPaid(itemUUID, ANY_OTHER_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.when_paid.toInstant() == ANY_OTHER_TIME
    }

    def 'paying should be idempotent on read side'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemIsPaid(itemUUID, ANY_OTHER_TIME)
        and:
            itemIsPaid(itemUUID, YET_ANOTHER_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.when_paid.toInstant() == ANY_OTHER_TIME
    }

    def 'should update item as payment missed'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemMarkedAsMissingPayment(itemUUID, ANY_OTHER_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.when_payment_marked_as_missing.toInstant() == ANY_OTHER_TIME
    }

    def 'updating item as payment missed should be idempotent'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME)
        and:
            itemMarkedAsMissingPayment(itemUUID, ANY_OTHER_TIME)
        and:
            itemMarkedAsMissingPayment(itemUUID, YET_ANOTHER_TIME)
        then:
            ShopItem item = readModel.getItemBy(itemUUID)
            item.when_payment_marked_as_missing.toInstant() == ANY_OTHER_TIME
    }

    void itemIsOrdered(UUID uuid, Instant when, Instant paymentTimeout = ANY_TIME_LATER) {
        sink.input().send(new GenericMessage<>(new ItemOrdered(uuid, when, paymentTimeout)))
    }

    void itemIsPaid(UUID uuid, Instant when) {
        sink.input().send(new GenericMessage<>(new ItemPaid(uuid, when)))
    }

    void itemMarkedAsMissingPayment(UUID uuid, Instant when) {
        sink.input().send(new GenericMessage<>(new ItemPaymentTimeout(uuid, when)))
    }

}
