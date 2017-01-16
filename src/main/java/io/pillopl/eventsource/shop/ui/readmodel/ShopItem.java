package io.pillopl.eventsource.shop.ui.readmodel;

import lombok.Data;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Date;

@Data
public class ShopItem {

    private Long id;
    private String uuid;
    private String status;
    private Timestamp when_ordered;
    private Timestamp when_paid;
    private Timestamp when_payment_timeout;
    private Timestamp when_payment_marked_as_missing;
    private Integer version_value;
    private Timestamp last_modified;

    public Date getLastModifiedDate() {
        return Date.from(last_modified.toInstant());
    }
}
