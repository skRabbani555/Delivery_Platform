package com.deliveryplatform.store.service;

import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import com.deliveryplatform.store.dto.StoreRequest;
import com.deliveryplatform.store.dto.StoreResponse;
import com.deliveryplatform.store.entity.Store;
import com.deliveryplatform.store.repo.StoreRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreService {

    private final StoreRepo storeRepository;
    private final MerchantRepository merchantRepository;

    public StoreService(
            StoreRepo storeRepository,
            MerchantRepository merchantRepository) {
        this.storeRepository = storeRepository;
        this.merchantRepository = merchantRepository;
    }

    public StoreResponse createStore(
            Long merchantId,
            StoreRequest request) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        Store store = Store.builder()
                .merchant(merchant)
                .name(request.name())
                .phone(request.phone())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .active(true)
                .build();

        Store savedStore = storeRepository.save(store);

        return toResponse(savedStore);
    }

    public List<StoreResponse> getStores(Long merchantId) {

        return storeRepository.findByMerchantId(merchantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StoreResponse toResponse(Store store) {

        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getPhone(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                store.getActive());
    }
}