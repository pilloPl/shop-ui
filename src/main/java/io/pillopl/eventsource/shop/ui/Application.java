package io.pillopl.eventsource.shop.ui;

import io.pillopl.eventsource.shop.ui.events.Event;
import io.pillopl.eventsource.shop.ui.readmodel.ReadModelUpdater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableBinding(Sink.class)
public class Application  {

    private final ReadModelUpdater readModelUpdater;

    @Autowired
    public Application(ReadModelUpdater readModelUpdater1) {
        this.readModelUpdater = readModelUpdater1;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
    }

    @StreamListener(Sink.INPUT)
    public void eventStream(Event event) {
        log.info("Received: " + event);
        readModelUpdater.handle(event);
    }

}
