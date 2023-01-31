package br.com.alura.ecommerce;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

//Produtor
public class NewOrderMain {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try (var orderDispatcher = new KafkaDispatcher<Order>()) {
            try (var emailDispatcher = new KafkaDispatcher<Email>()) { //para o e-mail
                for (var i = 0; i < 10; i++) {
                    String email = gerarVenda(orderDispatcher);
                    enviarEmail(emailDispatcher, email);
                }
            }
        }
    }

    private static void enviarEmail(KafkaDispatcher<Email> emailDispatcher, String email) throws ExecutionException, InterruptedException {
        var text = "Thank you for your order! We are processing your order!";
        var emailCode = new Email(text, "");
        emailDispatcher.send("ECOMMERCE_SEND_EMAIL", email, emailCode);
    }

    private static String gerarVenda(KafkaDispatcher<Order> orderDispatcher) throws ExecutionException, InterruptedException {
        var orderId = UUID.randomUUID().toString();
        var amount = BigDecimal.valueOf(Math.random() * 5000 + 1);
        var email = Math.random() + "@email.com";

        var order = new Order(orderId, amount, email);
        orderDispatcher.send("ECOMMERCE_NEW_ORDER", email, order);
        return email;
    }
}
