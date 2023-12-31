package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class KafkaService<T> implements Closeable {
    private final KafkaConsumer<String, Message<T>> consumer;
    private final ConsumerFunction parse;

    public KafkaService(String simpleName, String topic, ConsumerFunction parse, Map<String, String> properties) {
        this(parse, simpleName, properties);
        consumer.subscribe(Collections.singletonList(topic)); //se inscrevendo no tópico
    }

    public KafkaService(String simpleName, Pattern topic, ConsumerFunction parse, Map<String, String> properties) {
        this(parse, simpleName, properties);
        consumer.subscribe(topic); //se inscrevendo no tópico
    }

    private KafkaService(ConsumerFunction parse, String simpleName, Map<String, String> properties) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(getProperties(simpleName, properties)); //criando a propriedade e passando o tipo
    }


    public void run() throws ExecutionException, InterruptedException {
        while (true) {
            ConsumerRecords<String, Message<T>> records = consumer.poll(Duration.ofMillis(100)); //tempo de escuta

            if (!records.isEmpty()) {
                System.out.println("Encontrei " + records.count() + " registros.");

                for (var record : records) {
                    try {
                        parse.consume(record);
                    } catch (Exception e) {
                        e.printStackTrace();
                        var message = record.value();
                        dispatchDeadLetter(message);
                    }
                }
            }
        }
    }

    private void dispatchDeadLetter(Message<T> message) throws ExecutionException, InterruptedException {
        try (var deadLetter = new KafkaDispatcher<>()) {
            deadLetter.send(
                    "ECOMMERCE_DEADLETTER",
                    message.getId().toString(),
                    message.getId().continueWith("DeadLetter"),
                    new GsonSerializer<>().serialize("", message));
        }
    }

    private Properties getProperties(String simpleName, Map<String, String> overridProperties) {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); //Transformando de bytes para string
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GsonDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, simpleName);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");  //limitando o número de mensagens que o consumidor vai receber
        properties.putAll(overridProperties); // adiciona as outras propriedades

        return properties;
    }

    @Override
    public void close() {
        consumer.close();
    }
}
