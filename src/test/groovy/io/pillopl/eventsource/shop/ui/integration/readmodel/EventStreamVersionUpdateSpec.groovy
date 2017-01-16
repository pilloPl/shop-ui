package io.pillopl.eventsource.shop.ui.integration.readmodel

import io.pillopl.eventsource.shop.ui.integration.IntegrationSpec
import io.pillopl.eventsource.shop.ui.readmodel.JdbcReadModel
import io.pillopl.eventsource.shop.ui.readmodel.ShopItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.messaging.support.GenericMessage
import spock.lang.Subject

import java.time.Instant

import static java.time.Instant.parse

class EventStreamVersionUpdateSpec extends IntegrationSpec {

    private static final Instant ANY_TIME = parse("1995-10-23T10:12:35Z")
    private static final Instant ANY_TIME_LATER = parse("1995-10-23T10:12:35Z").plusSeconds(3600)
    private static final Instant YET_ANOTHER_TIME = ANY_TIME_LATER.plusSeconds(100)


    @Subject @Autowired JdbcReadModel readModel

    @Autowired Sink sink


    def 'should not return version when there is no event related to given shop item'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            readModel.getVersion(itemUUID) == null
        then:
            Exception e  = thrown()
            e instanceof EmptyResultDataAccessException
    }

    def 'should update version when item is ordered'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
        then:
            readModel.getVersion(itemUUID) == 1
    }

    def 'should update version when item is paid'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
            itemIsPaid(itemUUID, ANY_TIME, 2)

        then:
            readModel.getVersion(itemUUID) == 2
    }

    def 'should update version when item payment is missing and then paid'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
            itemMarkedAsMissingPayment(itemUUID, ANY_TIME, 2)
            itemIsPaid(itemUUID, ANY_TIME, 3)
        then:
            readModel.getVersion(itemUUID) == 3
    }

    def 'should not return last modification date when there is no event related to given shop item'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            readModel.getLastModifiedDate(itemUUID) == null
        then:
            Exception e  = thrown()
            e instanceof EmptyResultDataAccessException
    }

    def 'should update last modification when item is ordered'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
        then:
            readModel.getLastModifiedDate(itemUUID) == ANY_TIME
    }

    def 'should update last modification when item is paid'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
            itemIsPaid(itemUUID, ANY_TIME_LATER, 2)

        then:
            readModel.getLastModifiedDate(itemUUID) == ANY_TIME_LATER
    }

    def 'should update last modification when item payment is missing and then paid'() {
        given:
            UUID itemUUID = UUID.randomUUID()
        when:
            itemIsOrdered(itemUUID, ANY_TIME, 1)
            itemMarkedAsMissingPayment(itemUUID, ANY_TIME_LATER, 2)
            itemIsPaid(itemUUID, YET_ANOTHER_TIME, 3)
        then:
            readModel.getLastModifiedDate(itemUUID) == YET_ANOTHER_TIME
    }


    void itemIsOrdered(UUID uuid, Instant when, Integer version) {
        sink.input().send(new GenericMessage<>(
                itemOrderedInJson(uuid, when, ANY_TIME_LATER, version)))
    }

    void itemIsPaid(UUID uuid, Instant when, Integer version) {
        sink.input().send(new GenericMessage<>(itemPaidInJson(uuid, when, version)))
    }

    void itemMarkedAsMissingPayment(UUID uuid, Instant when, Integer version) {
        sink.input().send(new GenericMessage<>(itemTimeoutInJson(uuid, when, version)))
    }

    private static String itemOrderedInJson(UUID uuid, Instant when, Instant paymentTimeout, Integer version) {
        return "{\"type\":\"item.ordered\",\"uuid\":\"$uuid\",\"version\":\"$version\",\"when\":\"$when\",\"paymentTimeoutDate\":\"$paymentTimeout\"}"
    }

    private static String itemPaidInJson(UUID uuid, Instant when, Integer version) {
        return "{\"type\":\"item.paid\",\"uuid\":\"$uuid\",\"version\":\"$version\",\"when\":\"$when\"}"
    }

    private static String itemTimeoutInJson(UUID uuid, Instant when, Integer version) {
        return "{\"type\":\"item.payment.timeout\",\"uuid\":\"$uuid\",\"version\":\"$version\",\"when\":\"$when\"}"
    }

}
