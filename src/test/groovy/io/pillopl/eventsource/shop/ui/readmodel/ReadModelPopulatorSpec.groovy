package io.pillopl.eventsource.shop.ui.readmodel

import io.pillopl.eventsource.shop.ui.events.ItemOrdered
import io.pillopl.eventsource.shop.ui.events.ItemPaymentTimeout
import io.pillopl.eventsource.shop.ui.events.ItemPaid
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

import static java.time.Instant.now
import static java.util.UUID.randomUUID

class ReadModelPopulatorSpec extends Specification {

    private static final UUID ANY_UUID = randomUUID()
    private static final Instant ANY_DATE = now()
    private static final Instant ANY_PAYMENT_TIMEOUT = now()

    JdbcReadModel jdbcReadModel = Mock()

    @Subject
    ReadModelUpdater readModelUpdater = new ReadModelUpdater(jdbcReadModel)

    def 'should update or create ordered item when receiving bought item event'() {
        when:
            readModelUpdater.handle(new ItemOrdered(ANY_UUID, ANY_DATE, ANY_PAYMENT_TIMEOUT))
        then:
            1 * jdbcReadModel.updateOrCreateItemAsOrdered(ANY_UUID, ANY_DATE, ANY_PAYMENT_TIMEOUT)
    }

    def 'should update item when receiving item paid event'() {
        when:
            readModelUpdater.handle(new ItemPaid(ANY_UUID, ANY_DATE))
        then:
            1 * jdbcReadModel.updateItemAsPaid(ANY_UUID, ANY_DATE)
    }

    def 'should update item when receiving payment timeout event'() {
        when:
            readModelUpdater.handle(new ItemPaymentTimeout(ANY_UUID, ANY_DATE))
        then:
            1 * jdbcReadModel.updateItemAsPaymentMissing(ANY_UUID, ANY_DATE)
    }
}
