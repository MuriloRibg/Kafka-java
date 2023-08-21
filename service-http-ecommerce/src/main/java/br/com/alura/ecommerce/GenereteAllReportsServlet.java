package br.com.alura.ecommerce;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GenereteAllReportsServlet extends HttpServlet {
    private final KafkaDispatcher<String> batchDispatcher = new KafkaDispatcher<>();

    @Override
    public void destroy() {
        super.destroy();
        batchDispatcher.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            batchDispatcher.send("SEND_MASSAGE_TO_ALL_USERS", "USER_GENERATE_READING_REPORT", "USER_GENERATE_READING_REPORT");

            System.out.println("Sent generate report to all users.");
            resp.setStatus(HttpServletResponse.SC_OK); //Return to consumer
            resp.getWriter().println("Report requests generated.");
            
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new ServletException(e);
        }
    }
}

