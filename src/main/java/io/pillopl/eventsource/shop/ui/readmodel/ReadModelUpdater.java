package io.pillopl.eventsource.shop.ui.readmodel;

import io.pillopl.eventsource.shop.ui.events.Event;
import io.pillopl.eventsource.shop.ui.events.ItemBought;
import io.pillopl.eventsource.shop.ui.events.ItemPaid;
import io.pillopl.eventsource.shop.ui.events.ItemPaymentTimeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReadModelUpdater {

    private final JdbcReadModel jdbcReadModelUpdater;

    @Autowired
    ReadModelUpdater(JdbcReadModel jdbcReadModelUpdater) {
        this.jdbcReadModelUpdater = jdbcReadModelUpdater;
    }

    public void handle(Event event) {
        if (event instanceof ItemBought) {
            final ItemBought itemBought = (ItemBought) event;
            jdbcReadModelUpdater.updateOrCreateItemAsBought(event.uuid(), event.when(), itemBought.getPaymentTimeoutDate());
        } else if (event instanceof ItemPaid) {
            jdbcReadModelUpdater.updateItemAsPaid(event.uuid(), event.when());
        } else if (event instanceof ItemPaymentTimeout) {
            jdbcReadModelUpdater.updateItemAsPaymentMissing(event.uuid(), event.when());
        } else {
            throw new IllegalArgumentException("Cannot handle event " + event.getClass());
        }
    }

}
