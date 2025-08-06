package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.dto.AccountDTO;
import com.bankservice.bank_service.dto.TransactionDTO;
import com.bankservice.bank_service.service.AccountService;
import com.bankservice.bank_service.service.TransactionService;
import com.bankservice.bank_service.service.impl.CustomUserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/accounts")
    public String accounts(Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                model.addAttribute("accounts", accountService.getAccountsByUserId(userDetails.getId()));
                model.addAttribute("username", userDetails.getUsername());
            } else {
                model.addAttribute("accounts", accountService.getAllAccounts());
            }
            return "accounts";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las cuentas: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/accounts/new")
    public String newAccountForm(Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                model.addAttribute("username", userDetails.getUsername());
                model.addAttribute("userId", userDetails.getId());
                return "account-new";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/accounts/new")
    public String createAccount(@RequestParam String accountType,
                               @RequestParam(defaultValue = "0.0") Double initialBalance,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                
                AccountDTO accountDTO = new AccountDTO();
                accountDTO.setUserId(userDetails.getId());
                accountDTO.setBalance(BigDecimal.valueOf(initialBalance));
                // Generar número de cuenta automáticamente en el servicio
                
                AccountDTO createdAccount = accountService.createAccount(accountDTO);
                redirectAttributes.addFlashAttribute("success", "Cuenta creada exitosamente: " + createdAccount.getAccountNumber());
                return "redirect:/accounts";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear la cuenta: " + e.getMessage());
            return "redirect:/accounts/new";
        }
    }

    @GetMapping("/accounts/{id}")
    public String accountDetail(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                AccountDTO account = accountService.getById(id);
                
                // Verificar que la cuenta pertenece al usuario
                if (!account.getUserId().equals(userDetails.getId())) {
                    model.addAttribute("error", "No tienes acceso a esta cuenta.");
                    return "error";
                }
                
                // Obtener las últimas 5 transacciones de la cuenta
                List<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(id);
                List<TransactionDTO> recentTransactions = transactions.stream()
                        .limit(5)
                        .toList();
                
                model.addAttribute("account", account);
                model.addAttribute("transactions", recentTransactions);
                model.addAttribute("username", userDetails.getUsername());
                model.addAttribute("userFullName", userDetails.getUsername());
                model.addAttribute("userEmail", userDetails.getUsername());
                return "account-detail";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la cuenta: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam Long accountId, Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                AccountDTO account = accountService.getById(accountId);
                
                // Verificar que la cuenta pertenece al usuario
                if (!account.getUserId().equals(userDetails.getId())) {
                    model.addAttribute("error", "No tienes acceso a esta cuenta.");
                    return "error";
                }
                
                // Obtener todas las transacciones de la cuenta
                List<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(accountId);
                
                model.addAttribute("account", account);
                model.addAttribute("transactions", transactions);
                model.addAttribute("username", userDetails.getUsername());
                return "transactions";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las transacciones: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/transactions/new")
    public String newTransactionForm(@RequestParam Long accountId, Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                AccountDTO account = accountService.getById(accountId);
                
                // Verificar que la cuenta pertenece al usuario
                if (!account.getUserId().equals(userDetails.getId())) {
                    model.addAttribute("error", "No tienes acceso a esta cuenta.");
                    return "error";
                }
                
                // Obtener todas las cuentas para mostrar en el formulario de destino
                List<AccountDTO> allAccounts = accountService.getAllAccounts();
                // Filtrar la cuenta actual para que no aparezca como destino
                List<AccountDTO> destinationAccounts = allAccounts.stream()
                        .filter(acc -> !acc.getId().equals(accountId))
                        .toList();
                
                model.addAttribute("account", account);
                model.addAttribute("destinationAccounts", destinationAccounts);
                model.addAttribute("username", userDetails.getUsername());
                return "transfer";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario de transferencia: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/transactions/new")
    public String createTransaction(@RequestParam Long fromAccountId,
                                   @RequestParam Long toAccountId,
                                   @RequestParam BigDecimal amount,
                                   @RequestParam String description,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                AccountDTO fromAccount = accountService.getById(fromAccountId);
                
                // Verificar que la cuenta origen pertenece al usuario
                if (!fromAccount.getUserId().equals(userDetails.getId())) {
                    redirectAttributes.addFlashAttribute("error", "No tienes acceso a esta cuenta.");
                    return "redirect:/accounts";
                }
                
                // Verificar que hay suficiente saldo
                if (fromAccount.getBalance().compareTo(amount) < 0) {
                    redirectAttributes.addFlashAttribute("error", "Saldo insuficiente para realizar la transferencia.");
                    return "redirect:/transactions/new?accountId=" + fromAccountId;
                }
                
                // Crear la transacción
                TransactionDTO transactionDTO = new TransactionDTO();
                transactionDTO.setFromAccountId(fromAccountId);
                transactionDTO.setToAccountId(toAccountId);
                transactionDTO.setAmount(amount);
                transactionDTO.setDescription(description);
                
                transactionService.transfer(transactionDTO);
                redirectAttributes.addFlashAttribute("success", "Transferencia realizada exitosamente.");
                return "redirect:/accounts/" + fromAccountId;
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al realizar la transferencia: " + e.getMessage());
            return "redirect:/transactions/new?accountId=" + fromAccountId;
        }
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}
