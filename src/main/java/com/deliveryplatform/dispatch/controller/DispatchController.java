package com.deliveryplatform.dispatch.controller;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.dispatch.service.DispatchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/{deliveryId}/assign")
    public Delivery assignDriver(
            @PathVariable Long deliveryId) {

        return dispatchService.assignDriver(deliveryId);
    }
}