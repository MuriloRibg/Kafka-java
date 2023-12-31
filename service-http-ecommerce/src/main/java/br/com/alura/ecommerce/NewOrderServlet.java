package br.com.alura.ecommerce;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class NewOrderServlet extends HttpServlet {
    private final KafkaDispatcher<Order> orderDispatcher = new KafkaDispatcher<>();
    private final KafkaDispatcher<Email> emailDispatcher = new KafkaDispatcher<>();

    @Override
    public void destroy() {
        super.destroy();
        orderDispatcher.close();
        emailDispatcher.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            String email = gerarVenda(req, orderDispatcher);
            enviarEmail(emailDispatcher, email);
            System.out.println("New Order sent successfully.");

            resp.setStatus(HttpServletResponse.SC_OK); //Return to consumer
            resp.getWriter().println("New order sent");
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new ServletException(e);
        }
    }

    private static void enviarEmail(KafkaDispatcher<Email> emailDispatcher, String email) throws ExecutionException, InterruptedException {
        var text = "Thank you for your order! We are processing your order!";
        var emailCode = new Email(text, "");
        emailDispatcher.send("ECOMMERCE_SEND_EMAIL", email, new CorrelationId(NewOrderServlet.class.getSimpleName()), emailCode);
    }

    private static String gerarVenda(HttpServletRequest req, KafkaDispatcher<Order> orderDispatcher) throws ExecutionException, InterruptedException {
        var orderId = UUID.randomUUID().toString();

        var amount = new BigDecimal(req.getParameter("amount"));
        var email = req.getParameter("email");

        var order = new Order(orderId, amount, email);
        orderDispatcher.send("ECOMMERCE_NEW_ORDER", email, new CorrelationId(NewOrderServlet.class.getSimpleName()), order);
        return email;
    }
}

