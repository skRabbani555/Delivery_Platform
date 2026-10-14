package com.deliveryplatform.merchant.repo;

import com.deliveryplatform.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Merchant> findByApiKey(String apiKey);
}