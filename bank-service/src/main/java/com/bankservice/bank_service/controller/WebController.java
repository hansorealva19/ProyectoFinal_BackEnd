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
                
                // Obtener todas las cuentas del mismo banco para mostrar en el formulario de destino
                List<AccountDTO> allAccounts = accountService.getAllAccounts();
                // Filtrar solo cuentas del mismo banco y excluir la cuenta actual
                List<AccountDTO> destinationAccounts = allAccounts.stream()
                        .filter(acc -> !acc.getId().equals(accountId))
                        .filter(acc -> acc.getBankCode() != null && acc.getBankCode().equals(account.getBankCode()))
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
    
    @GetMapping("/admin/accounts")
    public String adminAccounts(Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                
                // Obtener todas las cuentas para administración
                List<AccountDTO> allAccounts = accountService.getAllAccounts();
                
                model.addAttribute("accounts", allAccounts);
                model.addAttribute("username", userDetails.getUsername());
                return "admin-accounts";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las cuentas: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/admin/accounts/{id}/changeBank")
    public String changeBankAccount(@PathVariable Long id,
                                   @RequestParam String bankCode,
                                   @RequestParam String bankName,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                // Actualizar la información bancaria de la cuenta
                AccountDTO updatedAccount = accountService.updateBankInfo(id, bankCode, bankName);
                redirectAttributes.addFlashAttribute("success", 
                    "Banco cambiado exitosamente a: " + bankName + " (" + bankCode + ") para la cuenta " + updatedAccount.getAccountNumber());
                return "redirect:/admin/accounts";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar banco: " + e.getMessage());
            return "redirect:/admin/accounts";
        }
    }
    
    @GetMapping("/transactions/interbank")
    public String newInterbankTransferForm(@RequestParam Long accountId, Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) authentication.getPrincipal();
                AccountDTO account = accountService.getById(accountId);
                
                // Verificar que la cuenta pertenece al usuario
                if (!account.getUserId().equals(userDetails.getId())) {
                    model.addAttribute("error", "No tienes acceso a esta cuenta.");
                    return "error";
                }
                
                model.addAttribute("account", account);
                model.addAttribute("username", userDetails.getUsername());
                return "interbank-transfer";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario de transferencia interbancaria: " + e.getMessage());
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
    
    @PostMapping("/transactions/interbank")
    public String createInterbankTransfer(@RequestParam Long fromAccountId,
                                         @RequestParam String bankCode,
                                         @RequestParam String accountNumber,
                                         @RequestParam String beneficiaryName,
                                         @RequestParam String beneficiaryDocument,
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
                
                // Verificar que hay suficiente saldo (incluyendo comisión interbancaria)
                BigDecimal interbankFee = amount.multiply(new BigDecimal("0.005")); // 0.5% de comisión
                BigDecimal totalAmount = amount.add(interbankFee);
                
                if (fromAccount.getBalance().compareTo(totalAmount) < 0) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Saldo insuficiente. Monto: $" + amount + " + Comisión: $" + interbankFee + " = Total: $" + totalAmount);
                    return "redirect:/transactions/interbank?accountId=" + fromAccountId;
                }
                
                // Verificar si la cuenta destino existe en nuestro sistema
                AccountDTO toAccount = null;
                try {
                    // Buscar cuenta por número de cuenta y código de banco
                    List<AccountDTO> allAccounts = accountService.getAllAccounts();
                    toAccount = allAccounts.stream()
                            .filter(acc -> acc.getAccountNumber().equals(accountNumber) && acc.getBankCode().equals(bankCode))
                            .findFirst()
                            .orElse(null);
                } catch (Exception e) {
                    // Si hay error, la cuenta no existe en nuestro sistema
                    toAccount = null;
                }
                
                if (toAccount != null) {
                    // Es una transferencia interna entre cuentas de nuestro sistema
                    TransactionDTO transactionDTO = new TransactionDTO();
                    transactionDTO.setFromAccountId(fromAccountId);
                    transactionDTO.setToAccountId(toAccount.getId());
                    transactionDTO.setAmount(amount); // Sin comisión para transferencias internas
                    transactionDTO.setDescription("TRANSFERENCIA INTERNA - " + 
                        "De: " + fromAccount.getAccountNumber() + " (" + fromAccount.getBankName() + ") - " +
                        "A: " + toAccount.getAccountNumber() + " (" + toAccount.getBankName() + ") - " +
                        "Beneficiario: " + beneficiaryName + " - " +
                        "Descripción: " + description);
                    
                    // Procesar la transferencia normal (débito y crédito)
                    transactionService.transfer(transactionDTO);
                    
                    redirectAttributes.addFlashAttribute("success", 
                        "✅ Transferencia completada entre bancos del grupo financiero. " +
                        "Monto transferido: $" + amount + " (sin comisión por ser transferencia interna)");
                } else {
                    // Es una transferencia realmente interbancaria (externa)
                    // Crear la transacción interbancaria
                    TransactionDTO transactionDTO = new TransactionDTO();
                    transactionDTO.setFromAccountId(fromAccountId);
                    transactionDTO.setToAccountId(null); // No hay cuenta destino en nuestro sistema
                    transactionDTO.setAmount(totalAmount); // Incluye la comisión
                    transactionDTO.setDescription("TRANSFERENCIA INTERBANCARIA - " + 
                        "Banco: " + bankCode + " - " +
                        "Cuenta: " + accountNumber + " - " +
                        "Beneficiario: " + beneficiaryName + " - " +
                        "Documento: " + beneficiaryDocument + " - " +
                        "Descripción: " + description);
                    
                    // Procesar la transferencia (solo débito de nuestra cuenta)
                    transactionService.transfer(transactionDTO);
                    
                    redirectAttributes.addFlashAttribute("success", 
                        "✅ Transferencia interbancaria externa procesada exitosamente. " +
                        "Monto transferido: $" + amount + " + Comisión: $" + interbankFee + " = Total debitado: $" + totalAmount);
                }
                return "redirect:/accounts/" + fromAccountId;
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al realizar la transferencia interbancaria: " + e.getMessage());
            return "redirect:/transactions/interbank?accountId=" + fromAccountId;
        }
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}
