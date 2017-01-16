package io.pillopl.eventsource.shop.ui.readmodel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@RestController
@Slf4j
public class ProjectionController {

    private final JdbcReadModel jdbcReadModel;
    public static final SimpleDateFormat HTTP_DATE_FORMAT =  new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");


    @Autowired
    public ProjectionController(JdbcReadModel readModel) {
        this.jdbcReadModel = readModel;
    }


    @RequestMapping(method = RequestMethod.GET)
    public List<Map<String, Object>> read(HttpRequest request) {
        return jdbcReadModel.readEverything();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
    public ResponseEntity<ShopItem> readById(@PathVariable String uuid, @RequestHeader(value="Expect") Integer expectedVersion) {
        final ShopItem item = jdbcReadModel.getItemBy(UUID.fromString(uuid));
        if (dataAtExpectedState(item, expectedVersion)) {
            return okWithLastModifiedDate(item);
        }
        return retrySoon();

    }

    private ResponseEntity<ShopItem> okWithLastModifiedDate(ShopItem item) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LAST_MODIFIED, HTTP_DATE_FORMAT.format(item.getLastModifiedDate()));
        return new ResponseEntity<>(item, headers, HttpStatus.OK);
    }

    private boolean dataAtExpectedState(ShopItem item, Integer expectedVersion) {
        return expectedVersion == null || expectedVersion <= item.getVersion_value();
    }

    private ResponseEntity<ShopItem> retrySoon() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Integer.toString(2));
        return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

}
