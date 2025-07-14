package com.uday.financeportal.repository;

import com.uday.financeportal.model.Transaction;
import com.uday.financeportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByDateDesc(User user);
}
