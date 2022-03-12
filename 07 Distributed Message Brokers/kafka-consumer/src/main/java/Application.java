import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Application {

    private static final String TOPIC = "events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

    public static void main(String[] args) {
        String consumerGroup = "defaultConsumerGroup";
        if (args.length == 1) {
            consumerGroup = args[0];
        }

        System.out.println("Consumer is part of consumer group " + consumerGroup);

        Consumer<Long, String> kafkaConsumer = createKafkaConsumer(BOOTSTRAP_SERVERS, consumerGroup);

        consumeMessages(TOPIC, kafkaConsumer);
    }

    public static void consumeMessages(String topic, Consumer<Long, String> kafkaConsumer) {

        /**
         * May subscribe more than one broker
         * */
        kafkaConsumer.subscribe(Collections.singletonList(topic));

        while (true) {

            /**
             * Duration define the timeout, note this will only poll messages created after subscribe to topics
             * */
            ConsumerRecords<Long, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));

            if (consumerRecords.isEmpty()) {
                // do something else
            }

            for (ConsumerRecord<Long, String> record : consumerRecords) {
                System.out.println(String.format("Received record (key: %d, value: %s, partition: %d, offset: %d",
                        record.key(), record.value(), record.partition(), record.offset()));
            }

            // do something with the records

            /**
             * Commit offsets returned on the last poll() for all the subscribed list of topics and partition
             *
             * Note if not commitAsync (or failed between successfully polled and commit), then next time it reach to
             * this publisher, publisher will still return messages polled before since its never committed
             * */
            kafkaConsumer.commitAsync();
        }
    }


    public static Consumer<Long, String> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);

        /**
         * Whenever a consumer consume a message, since it may broke before it finish processing, thus it is better to define
         * the finish process manually, so before it finish it can still be consumed by other consumer and held by kafka
         * */
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new KafkaConsumer<>(properties);
    }
}
