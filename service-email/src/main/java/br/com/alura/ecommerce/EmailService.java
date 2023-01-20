package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Map;

public class EmailService {
    public static void main(String[] args) {
        var emailsService = new EmailService();
        try (var service = new KafkaService(
                EmailService.class.getSimpleName(),
                "ECOMMERCE_SEND_EMAIL",
                emailsService::parse,
                Email.class,
                Map.of())) { // == new HashMap<>()
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Email> record) {
        System.out.println("-----------------------------------------");
        System.out.println("Send email!");
        System.out.println(record.key());
        System.out.println(record.value());
        System.out.println(record.partition());
        System.out.println(record.offset());

        try {
            Thread.sleep(1000); //colocando um tempo entre as thread para n estourar o processamento
        } catch (InterruptedException e) {
            // ignoring
            e.printStackTrace();
        }
        System.out.println("Email send");
    }
}
