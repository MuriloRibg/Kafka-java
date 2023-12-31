package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

// Consumidor
public class ReadingReportService {

    private static final Path SOURCE = new File("src/main/resources/report.txt").toPath();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var reportService = new ReadingReportService();
        try (var service = new KafkaService<User>(
                ReadingReportService.class.getSimpleName(),
                "ECOMMERCE_USER_GENERATE_READING_REPORT",
                reportService::parse,
                new HashMap<>())) { //o map é opcional, e por isso está vazio
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Message<User>> record) throws IOException {
        User user = record.value().getPayload();
        System.out.println("-----------------------------------------");
        System.out.println("Processing report for " + user);

        var target = new File(user.getReportPath());
        IO.copyTo(SOURCE, target);
        IO.append(target, " Created for " + user.getUuid());

        System.out.println("File created: " + target.getAbsolutePath());
    }
}
