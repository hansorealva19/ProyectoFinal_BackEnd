package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.OrderViewModel;
import org.springframework.stereotype.Service;
import java.util.List;
import com.ecommerce.frontend.service.OrderRestService;

@Service
public class OrderService {
    private final OrderRestService orderRestService;

    public OrderService(OrderRestService orderRestService) {
        this.orderRestService = orderRestService;
    }

    public java.util.Map<String,Object> getOrders(String username, int page, int size) {
        return orderRestService.getOrdersByUser(username, page, size, null);
    }

    public java.util.Map<String,Object> getOrders(String username, int page, int size, String jwt) {
        return orderRestService.getOrdersByUser(username, page, size, jwt);
    }
}
