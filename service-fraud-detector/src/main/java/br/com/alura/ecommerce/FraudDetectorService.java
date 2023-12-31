package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

// Consumidor
public class FraudDetectorService {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var fraudDetectorService = new FraudDetectorService();
        try (var service = new KafkaService<Order>(
                FraudDetectorService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                fraudDetectorService::parse,
                new HashMap<>())) { //o map é opcional, e por isso está vazio
            service.run();
        }
    }

    public final KafkaDispatcher<Order> orderKafkaDispatcher = new KafkaDispatcher<>();

    private void parse(ConsumerRecord<String, Message<Order>> record) throws ExecutionException, InterruptedException {
        Message<Order> message = record.value();

        System.out.println("-----------------------------------------");
        System.out.println("Processing new order, checking for fraud");
        System.out.println(record.key());
        System.out.println(message);
        System.out.println(record.partition());
        System.out.println(record.offset());

        try {
            Thread.sleep(1000); //colocando um tempo entre as thread para n estourar o processamento
        } catch (InterruptedException e) {
            // ignoring
            e.printStackTrace();
        }

        var order = message.getPayload();
        if (isFraud(order)) {
            System.out.println("Order is fraud!");
            orderKafkaDispatcher.send(
                    "ECOMMERCE_ORDER_REJECTED",
                    order.getEmail(),
                    message.getId().continueWith(FraudDetectorService.class.getSimpleName()),
                    order);
        } else {
            System.out.println("Order processed.");
            orderKafkaDispatcher.send(
                    "ECOMMERCE_ORDER_APPROVED",
                    order.getEmail(),
                    message.getId().continueWith(FraudDetectorService.class.getSimpleName()),
                    order);
        }
    }

    private static boolean isFraud(Order order) {
        return order.getAmount().compareTo(new BigDecimal("4500")) >= 0;
    }
}
