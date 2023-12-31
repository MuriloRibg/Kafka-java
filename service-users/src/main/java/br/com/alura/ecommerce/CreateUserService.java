package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.rmi.server.UID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CreateUserService {

    private final Connection connection;

    CreateUserService() throws SQLException {
        String url = "jdbc:sqlite:service-users/target/users_database.db";
        this.connection = DriverManager.getConnection(url);
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS USERS (UUID VARCHAR(200) PRIMARY KEY, EMAIL VARCHAR(200) NOT NULL)");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException, ExecutionException, InterruptedException {
        var createUserService = new CreateUserService();
        try (var service = new KafkaService<Order>(
                CreateUserService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                createUserService::parse,
                new HashMap<>())) { //o map é opcional, e por isso está vazio
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Message<Order>> record) throws SQLException {
        System.out.println("-----------------------------------------");
        System.out.println("Processing new order, checking for new user.");
        System.out.println(record.key());
        System.out.println(record.value());
        System.out.println(record.partition());
        System.out.println(record.offset());

        var order = record.value().getPayload();
        if (isNewUser(order.getEmail())) {
            insertNewUser(order.getEmail());
        }
    }

    private void insertNewUser(String email) throws SQLException {
        var insert = connection.prepareStatement("INSERT INTO USERS VALUES (?,?)");
        String uuid = UUID.randomUUID().toString();
        insert.setString(1, uuid);
        insert.setString(2, email);
        insert.execute();
        System.out.println("Usuário uuid: " + uuid + " e email: " + email + " adicionado!");
    }

    private boolean isNewUser(String email) throws SQLException {
        var exists = connection.prepareStatement("SELECT UUID FROM USERS WHERE EMAIL = ? LIMIT 1");
        exists.setString(1, email);
        var results = exists.executeQuery();
        return !results.next();
    }
}
