package io.pillopl.eventsource.shop.ui.integration.readmodel

import com.google.common.net.HttpHeaders
import io.pillopl.eventsource.shop.ui.integration.IntegrationSpec
import io.pillopl.eventsource.shop.ui.readmodel.JdbcReadModel
import io.pillopl.eventsource.shop.ui.readmodel.ProjectionController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Subject

import java.time.Instant

import static com.google.common.net.HttpHeaders.EXPECT
import static com.google.common.net.HttpHeaders.LAST_MODIFIED
import static io.pillopl.eventsource.shop.ui.readmodel.ProjectionController.HTTP_DATE_FORMAT
import static io.pillopl.eventsource.shop.ui.readmodel.ProjectionController.HTTP_DATE_FORMAT
import static java.time.Instant.parse
import static java.util.Date.from
import static java.util.Date.from
import static java.util.Date.from
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProjectionReading extends IntegrationSpec {

    private static final Instant ANY_TIME = parse("1995-10-23T10:12:35Z")
    private static final Instant ANY_TIME_LATER = parse("1995-10-23T10:12:35Z").plusSeconds(3600)


    @Subject
    @Autowired
    JdbcReadModel readModel

    @Autowired
    Sink sink

    @Autowired
    private WebApplicationContext wac

    private MockMvc mockMvc

    public def setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }


    def 'should ask to retry after when state is not yet present'() {
        given:
            UUID itemUUID = UUID.randomUUID()
            itemIsOrderedAtVersion(itemUUID, ANY_TIME, currentVersion)
        when:
            ResultActions result = mockMvc.perform(get("/${itemUUID}").header(EXPECT, expectedVersion))
        then:
            result.andExpect(header().longValue("Retry-After", 2))
            result.andExpect(status().isServiceUnavailable())
        where:
            currentVersion || expectedVersion
            1              || 2
            2              || 3
            4              || 10
            100            || 201
    }

    def 'should return the data when expected version is equal or smaller than current version'() {
        given:
            UUID itemUUID = UUID.randomUUID()
            itemIsOrderedAtVersion(itemUUID, ANY_TIME, currentVersion)
        when:
            ResultActions result = mockMvc.perform(get("/${itemUUID}").header(EXPECT, expectedVersion))
        then:
            result.andExpect(status().isOk())
        where:
            currentVersion || expectedVersion
            2              || 2
            3              || 3
            40             || 10
            1000           || 201
    }

    def 'should return the data after retrying'() {
        given:
            UUID itemUUID = UUID.randomUUID()
            itemIsOrderedAtVersion(itemUUID, ANY_TIME, 1)
        when:
            ResultActions result = mockMvc.perform(get("/${itemUUID}").header("Expect", 2))
        then:
            result.andExpect(header().longValue("Retry-After", 2))
            result.andExpect(status().isServiceUnavailable())
        when:
            itemIsPaidAtVersion(itemUUID, ANY_TIME, 2)
        and:
            ResultActions secondResult = mockMvc.perform(get("/${itemUUID}").header("Expect", 2))
        then:
            secondResult.andExpect(status().isOk())
    }

    def 'should return last modified date in response'() {
        given:
            UUID itemUUID = UUID.randomUUID()
            itemIsOrderedAtVersion(itemUUID, ANY_TIME, 1)
        when:
            ResultActions result = mockMvc.perform(get("/${itemUUID}").header("Expect", 1))
        then:
            result.andExpect(header().string(LAST_MODIFIED, HTTP_DATE_FORMAT.format(from(ANY_TIME))))
            result.andExpect(status().isOk())

    }

    void itemIsOrderedAtVersion(UUID uuid, Instant when, Integer version) {
        sink.input().send(new GenericMessage<>(
                itemOrderedInJson(uuid, when, ANY_TIME_LATER, version)))
    }

    void itemIsPaidAtVersion(UUID uuid, Instant when, Integer version) {
        sink.input().send(new GenericMessage<>(itemPaidInJson(uuid, when, version)))
    }

    private static String itemOrderedInJson(UUID uuid, Instant when, Instant paymentTimeout, Integer version) {
        return "{\"type\":\"item.ordered\",\"uuid\":\"$uuid\",\"version\":\"$version\",\"when\":\"$when\",\"paymentTimeoutDate\":\"$paymentTimeout\"}"
    }

    private static String itemPaidInJson(UUID uuid, Instant when, Integer version) {
        return "{\"type\":\"item.paid\",\"uuid\":\"$uuid\",\"version\":\"$version\",\"when\":\"$when\"}"
    }

}
