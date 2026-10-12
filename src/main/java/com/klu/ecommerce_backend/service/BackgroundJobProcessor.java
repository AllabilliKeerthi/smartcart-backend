
package com.klu.ecommerce_backend.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klu.ecommerce_backend.model.BackgroundJob;
import com.klu.ecommerce_backend.model.Order;
import com.klu.ecommerce_backend.repository.BackgroundJobRepository;
import com.klu.ecommerce_backend.repository.OrderRepository;

@Service
public class BackgroundJobProcessor {

    private final BackgroundJobRepository jobRepository;
    private final JobNotificationService notifier;
    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;

    @Value("${smartcart.mail.from:${spring.mail.username:no-reply@example.com}}")
    private String mailFrom;

    public BackgroundJobProcessor(
            BackgroundJobRepository jobRepository,
            JobNotificationService notifier,
            JavaMailSender mailSender,
            OrderRepository orderRepository) {

        this.jobRepository = jobRepository;
        this.notifier = notifier;
        this.mailSender = mailSender;
        this.orderRepository = orderRepository;
    }

    @Async("taskExecutor")
    public void process(Long jobId) {

        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Background job not found: " + jobId));

        try {
            update(job, "PROCESSING", "Task started");

            switch (job.getJobType()) {
                case "EMAIL" -> sendOrderEmail(job);
                case "INVOICE" -> generateInvoice(job);
                case "NOTIFICATION" -> sendNotification(job);
                default -> throw new RuntimeException(
                        "Unknown job type: " + job.getJobType());
            }

            update(job, "COMPLETED", "Task completed successfully");

        } catch (Exception e) {
            String errorMessage = e.getMessage() != null
                    ? e.getMessage()
                    : "Unknown background job error";

            update(job, "FAILED", errorMessage);

            System.err.println(
                    "Background job " + jobId + " failed: " + errorMessage);
        }
    }

    @Transactional
    public void update(
            BackgroundJob job,
            String status,
            String message) {

        job.setStatus(status);
        job.setMessage(message);

        jobRepository.save(job);
        notifier.publish(job);
    }

    private void sendOrderEmail(BackgroundJob job) {

        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found: " + job.getOrderId()));

        if (order.getUser() == null
                || order.getUser().getEmail() == null
                || order.getUser().getEmail().isBlank()) {

            throw new RuntimeException(
                    "Customer email is missing for order "
                            + job.getOrderId());
        }

        String customerEmail = order.getUser().getEmail();

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailFrom);
        mail.setTo(customerEmail);
        mail.setSubject("SmartCart Order Confirmation");

        mail.setText(
                "Dear " + order.getUser().getName() + ",\n\n"
                + "Your order " + order.getOrderId()
                + " has been confirmed successfully.\n"
                + "Total Amount: ₹" + order.getTotalAmount() + "\n\n"
                + "Thank you for shopping with SmartCart!");

        mailSender.send(mail);
    }

    private void generateInvoice(BackgroundJob job) {

        try {
            Path folder = Paths.get("invoices");
            Files.createDirectories(folder);

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>SmartCart Invoice</title>
                    </head>
                    <body>
                        <h1>SmartCart Invoice</h1>
                        <p>Order ID: %d</p>
                        <p>Payment: Successful</p>
                        <p>Thank you for shopping with SmartCart!</p>
                    </body>
                    </html>
                    """.formatted(job.getOrderId());

            Path invoicePath = folder.resolve(
                    "invoice-" + job.getOrderId() + ".html");

            Files.writeString(
                    invoicePath,
                    html,
                    StandardCharsets.UTF_8);

            System.out.println(
                    "Invoice generated: "
                            + invoicePath.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Invoice generation failed for order "
                            + job.getOrderId(), e);
        }
    }

    private void sendNotification(BackgroundJob job) {

        System.out.println(
                "SmartCart notification: Order "
                        + job.getOrderId()
                        + " payment confirmed");
    }
}
