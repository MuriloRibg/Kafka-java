package br.com.alura.ecommerce;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaDispatcher<T> implements Closeable {
    private final KafkaProducer<String, Message<T>> producer;

    KafkaDispatcher() {
        this.producer = new KafkaProducer<>(properties());
    }

    private static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092"); //criando as config de consumidor
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); //transformar as strings em bytes
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GsonSerializer.class.getName()); //mensagem
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); //Garantir que a mensagem foi enviada para todos os brokers
        return properties;
    }

    public void send(String topic, String key, CorrelationId correlationId, T payload) throws ExecutionException, InterruptedException {
        Future<RecordMetadata> result = sendAsync(topic, key, correlationId, payload);
        result.get(); //Faz aquardar o retorno da messagem.
    }

    public Future<RecordMetadata> sendAsync(String topic, String key, CorrelationId correlationId, T payload) {
        var value = new Message<>(correlationId, payload);
        var record = new ProducerRecord<>(topic, key, value); //local onde vai ser salvo as mensagens - (nome do tópico)

        Callback callback = (data, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                return;
            }
            System.out.println("Sucesso enviando -> " + data.topic() + " :::partition " + data.partition() + " || offset: " + data.offset() + " || timestamp: " + data.timestamp());
        };

        Future<RecordMetadata> result = producer.send(record, callback);
        return result;
    }

    @Override
    public void close() {
        producer.close();
    }
}
