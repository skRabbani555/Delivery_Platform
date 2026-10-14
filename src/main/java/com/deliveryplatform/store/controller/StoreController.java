package com.deliveryplatform.store.controller;

import com.deliveryplatform.store.dto.StoreRequest;
import com.deliveryplatform.store.dto.StoreResponse;
import com.deliveryplatform.store.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(
            @PathVariable Long merchantId,
            @Valid @RequestBody StoreRequest request) {
        return storeService.createStore(merchantId, request);
    }

    @GetMapping
    public List<StoreResponse> getStores(
            @PathVariable Long merchantId) {
        return storeService.getStores(merchantId);
    }
}