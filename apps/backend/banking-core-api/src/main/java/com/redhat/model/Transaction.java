package main.java.com.redhat.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Transaction {
    public Long id;
    public Double amount;
    public String currency;
    public String status;

    public Transaction(Long id, Double amount, String currency, String status) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }
}