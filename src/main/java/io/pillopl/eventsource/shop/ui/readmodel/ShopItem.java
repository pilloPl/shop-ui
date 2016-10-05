package io.pillopl.eventsource.shop.ui.readmodel;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ShopItem {

    private Long id;
    private String uuid;
    private String status;
    private Timestamp when_ordered;
    private Timestamp when_paid;
    private Timestamp when_payment_timeout;
    private Timestamp when_payment_marked_as_missing;
}
