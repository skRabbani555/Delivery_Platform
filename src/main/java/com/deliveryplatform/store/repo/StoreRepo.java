package com.deliveryplatform.store.repo;

import com.deliveryplatform.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepo extends JpaRepository<Store, Long> {

    List<Store> findByMerchantId(Long merchantId);
}