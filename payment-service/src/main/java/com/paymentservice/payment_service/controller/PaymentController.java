package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.dto.PaymentRequestDTO;
import com.paymentservice.payment_service.dto.PaymentResponseDTO;
import com.paymentservice.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<EntityModel<PaymentResponseDTO>> processPayment(@RequestBody PaymentRequestDTO requestDTO) {
        PaymentResponseDTO response = paymentService.processPayment(requestDTO);
        EntityModel<PaymentResponseDTO> resource = EntityModel.of(response);
        resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(PaymentController.class).processPayment(requestDTO)).withSelfRel());
        return ResponseEntity.ok(resource);
    }
}
