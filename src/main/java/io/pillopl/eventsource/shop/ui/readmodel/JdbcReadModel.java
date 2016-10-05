package io.pillopl.eventsource.shop.ui.readmodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

import static java.sql.Timestamp.from;

@Component
class JdbcReadModel {

    private static final String UPDATE_ORDERED_ITEM_SQL
            = "UPDATE items SET when_payment_timeout = ?, status = 'ORDERED' WHERE uuid = ?";

    private static final String INSERT_ORDERED_ITEM_SQL =
            "INSERT INTO items " +
                    "(id, uuid, status, when_ordered, when_paid, when_payment_timeout, when_payment_marked_as_missing)" +
                    " VALUES (items_seq.nextval, ?, 'ORDERED', ?, NULL, ?, NULL)";

    private static final String UPDATE_PAID_ITEM_SQL
            = "UPDATE items SET when_paid = ?, status = 'PAID' WHERE when_paid IS NULL AND uuid = ?";

    private static final String UPDATE_PAYMENT_MISSING_SQL
            = "UPDATE items SET when_payment_marked_as_missing = ?, status = 'PAYMENT_MISSING' WHERE when_payment_marked_as_missing IS NULL AND uuid = ?";

    private static final String QUERY_FOR_ITEM_SQL =
            "SELECT * FROM items WHERE uuid = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void updateOrCreateItemAsOrdered(UUID uuid, Instant when, Instant paymentTimeoutDate) {
        final int affectedRows = jdbcTemplate.update(UPDATE_ORDERED_ITEM_SQL, from(paymentTimeoutDate), uuid);
        if (affectedRows == 0) {
            jdbcTemplate.update(INSERT_ORDERED_ITEM_SQL, uuid, from(when), from(paymentTimeoutDate));
        }
    }

    void updateItemAsPaid(UUID uuid, Instant when) {
        jdbcTemplate.update(UPDATE_PAID_ITEM_SQL, from(when), uuid);
    }

    void updateItemAsPaymentMissing(UUID uuid, Instant when) {
        jdbcTemplate.update(UPDATE_PAYMENT_MISSING_SQL, from(when), uuid);
    }

    ShopItem getItemBy(UUID uuid) {
        return jdbcTemplate.queryForObject(QUERY_FOR_ITEM_SQL, new Object[]{uuid}, new BeanPropertyRowMapper<>(ShopItem.class));
    }
}
