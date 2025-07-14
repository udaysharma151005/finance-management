package com.uday.financeportal.controller;

import com.uday.financeportal.model.Transaction;
import com.uday.financeportal.model.User;
import com.uday.financeportal.repository.TransactionRepository;
import com.uday.financeportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            Model model,
            Principal principal
    ) {
        // 🧠 Get current user
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        // 📦 All transactions
        List<Transaction> all = transactionRepository.findByUserOrderByDateDesc(user);

        // 🔍 Filter based on type
        List<Transaction> filtered = switch (type.toUpperCase()) {
            case "INCOME" -> all.stream().filter(t -> "INCOME".equals(t.getType())).toList();
            case "EXPENSE" -> all.stream().filter(t -> "EXPENSE".equals(t.getType())).toList();
            default -> all;
        };

        // ➕ Totals
        double income = filtered.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = filtered.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // 📊 Add to model
        model.addAttribute("transactions", filtered);
        model.addAttribute("income", income);
        model.addAttribute("expense", expense);
        model.addAttribute("balance", income - expense);
        model.addAttribute("newTransaction", new Transaction());
        model.addAttribute("type", type);
        model.addAttribute("username", user.getName()); // ✅ Inject user's name

        return "dashboard";
    }

    @GetMapping("/budget")
    public String showBudgetPage(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        double monthlyBudget = user.getMonthlyBudget() != null ? user.getMonthlyBudget() : 0.0;

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        double totalExpenseThisMonth = transactionRepository.findByUserOrderByDateDesc(user).stream()
                .filter(t -> t.getDate() != null
                        && t.getDate().getMonthValue() == month
                        && t.getDate().getYear() == year
                        && "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double remaining = monthlyBudget - totalExpenseThisMonth;

        model.addAttribute("monthlyBudget", monthlyBudget);
        model.addAttribute("totalExpenseThisMonth", totalExpenseThisMonth);
        model.addAttribute("remainingBudget", remaining);

        return "budget"; // matches budget.html
    }

    @PostMapping("/add-transaction")
    public String addTransaction(@ModelAttribute Transaction newTransaction, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        newTransaction.setUser(user);

        if (newTransaction.getDate() == null) {
            newTransaction.setDate(LocalDate.now());
        }

        transactionRepository.save(newTransaction);
        return "redirect:/dashboard";
    }

    @GetMapping("/delete-transaction/{id}")
    public String deleteTransaction(@PathVariable Long id, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Transaction t = transactionRepository.findById(id).orElseThrow();

        if (t.getUser().getId().equals(user.getId())) {
            transactionRepository.delete(t);
        }

        return "redirect:/dashboard";
    }
}
