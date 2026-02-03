package com.redhat.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionDTO {
    public Long id;
    public Double amount;
    public String currency;
    public String status;
    public String accountId;
}