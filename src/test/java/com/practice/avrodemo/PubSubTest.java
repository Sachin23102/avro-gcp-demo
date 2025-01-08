package com.practice.avrodemo;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.practice.avrodemo.avro.ContactId;
import com.practice.avrodemo.avro.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PubSubTest {

    @Autowired
    PubSubTemplate pubSubTemplate;

    @Test
    void canSendMessageViaPubSubTemplate() throws ExecutionException, InterruptedException {
        var userRecord = User.newBuilder()
                .setContactId(new ContactId(UUID.randomUUID()))
                .setUsername("sachin")
                .setEmail("test@email.com") // or .setEmail(null)
                //                .setEmploymentStatus(EmploymentStatus.SELF_EMPLOYED)
                .build();
        var response = pubSubTemplate.publish(
                "sachin-avro-demo.employee", userRecord, Map.of("content-type", "application/avro"));
        System.out.println(response.get());
    }

    @Test
    void canReceiveMessageViaPubSubTemplate() throws InterruptedException {
        System.out.println("Subscribing to topic");
        pubSubTemplate.subscribeAndConvert(
                "sachin-avro-demo.employee",
                (message) -> {
                    System.out.println("Received message: " + message.getPayload());
                    //                    message.ack();
                },
                User.class);
        Thread.sleep(6000);
    }
}
