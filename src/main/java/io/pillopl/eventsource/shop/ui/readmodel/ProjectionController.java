package io.pillopl.eventsource.shop.ui.readmodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
public class ProjectionController {

    private static final String SELECT_WHOLE_PROJECTION = "select uuid, status, when_ordered, when_paid, when_payment_timeout, when_payment_marked_as_missing from items order by when_ordered";
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProjectionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @RequestMapping(method = RequestMethod.GET)
    public List<Map<String, Object>> read() {
        return jdbcTemplate.queryForList(SELECT_WHOLE_PROJECTION);
    }

}
