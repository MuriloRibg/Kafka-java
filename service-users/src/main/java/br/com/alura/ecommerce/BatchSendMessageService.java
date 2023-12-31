package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BatchSendMessageService {

    private final Connection connection;

    BatchSendMessageService() throws SQLException {
        String url = "jdbc:sqlite:service-users/target/users_database.db";
        this.connection = DriverManager.getConnection(url);
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS USERS (UUID VARCHAR(200) PRIMARY KEY, EMAIL VARCHAR(200) NOT NULL)");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException, ExecutionException, InterruptedException {
        var batchSendMessageService = new BatchSendMessageService();
        try (var service = new KafkaService<>(
                BatchSendMessageService.class.getSimpleName(),
                "ECOMMERCE_SEND_MASSAGE_TO_ALL_USERS",
                batchSendMessageService::parse,
                new HashMap<>())) { //o map é opcional, e por isso está vazio
            service.run();
        }
    }

    private final KafkaDispatcher<User> userKafkaDispatcher = new KafkaDispatcher<>();

    private void parse(ConsumerRecord<String, Message<String>> record) throws SQLException {
        Message<String> message = record.value();

        System.out.println("-----------------------------------------");
        System.out.println("Processing new batch");
        System.out.println("Topic: " + record.key());
        System.out.println(message);
        System.out.println(record.partition());
        System.out.println(record.offset());

        for (User user : getAllUsers()) {
            userKafkaDispatcher.sendAsync(
                    message.getPayload(),
                    user.getUuid(),
                    message.getId().continueWith(BatchSendMessageService.class.getSimpleName()),
                    user);
        }
    }

    private List<User> getAllUsers() throws SQLException {
        var results = connection.prepareStatement("SELECT UUID FROM USERS").executeQuery();
        List<User> users = new ArrayList<>();
        while (results.next()) {
            users.add(new User(results.getString(1)));
        }
        return users;
    }
}
