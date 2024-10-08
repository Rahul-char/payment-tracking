package com.example.payments.service;

import com.example.payments.dto.Paymentdto;
import com.example.payments.model.Payment;
import com.example.payments.repository.PaymentRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    public Payment initiatePayment(Paymentdto payment) {
        Payment p=Payment.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .username(payment.getUsername())
                .ponumber(payment.getPonumber())
                .invoicenumber(payment.getInvoicenumber())
                .targetBankAccount(payment.getTargetBankAccount())
                .tds(payment.getTds())
                .sourceBankAccount(payment.getSourceBankAccount())
                .status(payment.getStatus())
                .paymentdate(payment.getPaymentdate())
                .build();
        return paymentRepository.save(p);
    }
    // Method to initiate a list of payments
    public List<Payment> initiatePayments(List<Paymentdto> payments) {
        List<Payment> paymentList = payments.stream().map(payment -> Payment.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .username(payment.getUsername())
                .ponumber(payment.getPonumber())
                .invoicenumber(payment.getInvoicenumber())
                .targetBankAccount(payment.getTargetBankAccount())
                .tds(payment.getTds())
                .sourceBankAccount(payment.getSourceBankAccount())
                .status(payment.getStatus())
                .paymentdate(payment.getPaymentdate())
                .build()).collect(Collectors.toList());

        return paymentRepository.saveAll(paymentList);
    }
    // 1. Find pending payments
    public List<Payment> findPendingPayments() {
        return paymentRepository.findByStatus("PENDING");
    }

    // 2. Find total amount
    public Double getTotalAmount() {
        return paymentRepository.sumAllAmounts();
    }

    // 3. Find amount by invoice number
    public Double getAmountByInvoiceNumber(String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoicenumber(invoiceNumber);
        return payment != null ? payment.getAmount() : 0.0;
    }

    // 4. Find complete and pending payments by payment date
    public Map<String, List<Payment>> getPaymentsByStatusAndDate(String paymentDate) {
        Map<String, List<Payment>> paymentsByStatus = new HashMap<>();
        paymentsByStatus.put("completed", paymentRepository.findByPaymentdateAndStatus(paymentDate, "PAID"));
        paymentsByStatus.put("pending", paymentRepository.findByPaymentdateAndStatus(paymentDate, "PENDING"));
        return paymentsByStatus;
    }

    // 5. Edit payment
    public Payment editPayment(String id, Paymentdto paymentdto) {
        Optional<Payment> optionalPayment = paymentRepository.findById(id);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setAmount(paymentdto.getAmount());
            payment.setCurrency(paymentdto.getCurrency());
            payment.setUsername(paymentdto.getUsername());
            payment.setPonumber(paymentdto.getPonumber());
            payment.setInvoicenumber(paymentdto.getInvoicenumber());
            payment.setTargetBankAccount(paymentdto.getTargetBankAccount());
            payment.setSourceBankAccount(paymentdto.getSourceBankAccount());
            payment.setTds(paymentdto.getTds());
            payment.setStatus(paymentdto.getStatus());
            payment.setPaymentdate(paymentdto.getPaymentdate());
            return paymentRepository.save(payment);
        }
        throw new RuntimeException("Payment not found");
    }

    // 6. Delete payment
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    public Map<String, List<Double>> finalAmount() {
           List<Payment> p = paymentRepository.findAll();

        return p.stream()
                .collect(Collectors.groupingBy(
                        Payment::getInvoicenumber, // Correct reference to the method
                        Collectors.mapping(
                                payment -> (payment.getAmount() / 100) * (100 - payment.getTds()), // Calculate final amount
                                Collectors.toList() // Collect the calculated amounts into a list
                        )
                ));
    }


    public byte[] generateReport(String invoiceNumber) throws JRException {

        InputStream i = getClass().getResourceAsStream("/dummy.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(i);

        List<Payment> p = paymentRepository.findAll();
        List<Payment> filteredPayments = p.stream()
                            .filter(payment -> payment.getInvoicenumber().equals(invoiceNumber))
                            .collect(Collectors.toList());


        Map<String, Object> parameters = new HashMap<>();
        parameters.put("invoiceNumber", invoiceNumber);

        // Create a data source from your filtered data list

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(filteredPayments);

        // Fill the report

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        String outputDir = "C:/Users/Administrator/Documents/";

        String fileName = invoiceNumber + ".pdf";

        String filePath = outputDir + fileName;

        // Ensure the directory exists

        new File(outputDir).mkdirs();

        // Export the report to PDF and save it to the specified location

        JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);

        // Export the report to PDF and return as byte array

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
