package com.redhat.service;

import com.redhat.infrastructure.client.BankingCoreClient;
import com.redhat.model.TransactionDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class SettlementService {

    private static final Logger LOGGER = Logger.getLogger(SettlementService.class);

    @Inject
    @RestClient
    BankingCoreClient bankingClient;

    public void runSettlementProcess() {
        LOGGER.info("Starting automated settlement process using Service Account...");

        try {
            List<TransactionDTO> pending = bankingClient.getPendingTransactions();
            LOGGER.infof("Found %d pending transactions to process.", pending.size());

            pending.forEach(transaction -> {
                LOGGER.infof("Processing reconciliation for Transaction ID: %d", transaction.id);
                bankingClient.reconcileTransaction(transaction.id);
            });

            LOGGER.info("Settlement process completed successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to execute settlement process", e);
            throw e;
        }
    }
}