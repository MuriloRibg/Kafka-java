package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
    private final KafkaConsumer<String, T> consumer;
    private final ConsumerFunction parse;

    public KafkaService(String simpleName, String topic, ConsumerFunction parse, Class<T> classType, Map<String, String> properties) {
        this(parse, simpleName, classType, properties);
        consumer.subscribe(Collections.singletonList(topic)); //se inscrevendo no tópico
    }

    public KafkaService(String simpleName, Pattern topic, ConsumerFunction parse, Class<T> classType, Map<String, String> properties) {
        this(parse, simpleName, classType, properties);
        consumer.subscribe(topic); //se inscrevendo no tópico
    }

    private KafkaService(ConsumerFunction parse, String simpleName, Class<T> classType, Map<String, String> properties) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(getProperties(classType, simpleName, properties)); //criando a propriedade e passando o tipo
    }



    public void run() {
        while (true) {
            var records = consumer.poll(Duration.ofMillis(100)); //tempo de escuta

            if (!records.isEmpty()) {
                System.out.println("Encontrei " + records.count() + " registros.");

                for (var record : records) {
                    try {
                        parse.consume(record);
                    } catch (Exception e) {
                        //N para a aplicação e mostra somente o erro;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Properties getProperties(Class<T> classType, String simpleName, Map<String, String> overridProperties) {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); //Transformando de bytes para string
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GsonDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, simpleName);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        properties.putAll(overridProperties); // adiciona as outras propriedades

        return properties;
    }

    @Override
    public void close() {
        consumer.close();
    }
}
