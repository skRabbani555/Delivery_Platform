package com.deliveryplatform.e2e;

import com.deliveryplatform.delivery.dto.DeliveryCompletionRequest;
import com.deliveryplatform.delivery.dto.DeliveryRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.entity.DeliveryType;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.driver.dto.*;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.repo.DriverPayoutRepo;
import com.deliveryplatform.driver.repo.DriverRepo;
import com.deliveryplatform.merchant.dto.MerchantRequest;
import com.deliveryplatform.merchant.dto.MerchantResponse;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import com.deliveryplatform.store.dto.StoreRequest;
import com.deliveryplatform.store.dto.StoreResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class EndToEndDeliveryFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private DeliveryRepo deliveryRepo;

    @Autowired
    private DriverRepo driverRepo;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private DriverPayoutRepo driverPayoutRepo;

    @Test
    @DisplayName("Complete End-to-End Flow: Merchant Register -> Driver Online -> Dispatch -> Delivery Steps -> OTP Completion -> Earnings")
    void testCompleteEndToEndDeliveryWorkflow() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // ─────────────────────────────────────────────────────────────
        // 1. REGISTER MERCHANT
        // ─────────────────────────────────────────────────────────────
        String merchantEmail = "merchant_" + uniqueSuffix + "@test.com";
        String merchantPhone = "98" + String.format("%08d", Math.abs(uniqueSuffix.hashCode() % 100000000));
        MerchantRequest merchantReq = new MerchantRequest(
                "Fresh Pizza " + uniqueSuffix,
                merchantEmail,
                merchantPhone,
                "securePass123",
                "https://webhook.site/" + uniqueSuffix
        );

        MvcResult merchantRes = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantReq)))
                .andExpect(status().isCreated())
                .andReturn();

        MerchantResponse merchant = objectMapper.readValue(
                merchantRes.getResponse().getContentAsString(),
                MerchantResponse.class
        );

        assertNotNull(merchant.apiKey(), "Merchant API key should be generated");
        String apiKey = merchant.apiKey();

        // Login as merchant to get Bearer JWT token for store creation
        var loginPayload = objectMapper.createObjectNode()
                .put("email", merchantEmail)
                .put("password", "securePass123");

        MvcResult merchantLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String merchantJwt = objectMapper.readTree(merchantLoginRes.getResponse().getContentAsString())
                .get("accessToken").asText();

        // ─────────────────────────────────────────────────────────────
        // 2. CREATE MERCHANT STORE
        // ─────────────────────────────────────────────────────────────
        StoreRequest storeReq = new StoreRequest(
                "Downtown Branch " + uniqueSuffix,
                "9123456789",
                "100 Market St, San Francisco, CA",
                37.7749,
                -122.4194
        );

        MvcResult storeRes = mockMvc.perform(post("/api/v1/merchants/" + merchant.id() + "/stores")
                        .header("Authorization", "Bearer " + merchantJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeReq)))
                .andExpect(status().isCreated())
                .andReturn();

        StoreResponse store = objectMapper.readValue(
                storeRes.getResponse().getContentAsString(),
                StoreResponse.class
        );
        Long storeId = store.id();

        // ─────────────────────────────────────────────────────────────
        // 3. REGISTER & LOGIN DRIVER
        // ─────────────────────────────────────────────────────────────
        String driverEmail = "driver_" + uniqueSuffix + "@test.com";
        String driverPhone = "96" + String.format("%08d", Math.abs(uniqueSuffix.hashCode() % 100000000));
        DriverRequest driverReq = new DriverRequest(
                "David Rider " + uniqueSuffix,
                driverEmail,
                driverPhone,
                "driverPass123"
        );

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated());

        DriverLoginRequest driverLoginReq = new DriverLoginRequest(driverEmail, "driverPass123");

        MvcResult driverLoginRes = mockMvc.perform(post("/api/v1/driver/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverLoginReq)))
                .andExpect(status().isOk())
                .andReturn();

        DriverLoginResponse driverLogin = objectMapper.readValue(
                driverLoginRes.getResponse().getContentAsString(),
                DriverLoginResponse.class
        );
        String driverToken = driverLogin.accessToken();

        // ─────────────────────────────────────────────────────────────
        // 4. SET DRIVER AVAILABLE & UPDATE GPS LOCATION
        // ─────────────────────────────────────────────────────────────
        mockMvc.perform(patch("/api/v1/drivers/me/status")
                        .header("Authorization", "Bearer " + driverToken)
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk());

        // Place driver close to the store (e.g., 0.5 km away)
        UpdateLocationRequest locReq = new UpdateLocationRequest(37.7750, -122.4190);
        mockMvc.perform(post("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk());

        // ─────────────────────────────────────────────────────────────
        // 5. MERCHANT CREATES DELIVERY (via API Key & Idempotency Key)
        // ─────────────────────────────────────────────────────────────
        String externalOrderId = "ORD-" + uniqueSuffix;
        DeliveryRequest deliveryReq = new DeliveryRequest(
                storeId,
                externalOrderId,
                "100 Market St, San Francisco, CA",
                37.7749,
                -122.4194,
                "500 Howard St, San Francisco, CA",
                37.7880,
                -122.3990,
                "Alice Customer",
                "9876543210",
                1.5,
                DeliveryType.INSTANT
        );

        MvcResult deliveryRes = mockMvc.perform(post("/api/v1/deliveries")
                        .header("X-API-Key", apiKey)
                        .header("Idempotency-Key", "idemp-" + uniqueSuffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        DeliveryResponse delivery = objectMapper.readValue(
                deliveryRes.getResponse().getContentAsString(),
                DeliveryResponse.class
        );

        assertNotNull(delivery.id());
        assertNotNull(delivery.deliveryOtp(), "Merchant should receive OTP for the customer");
        String customerOtp = delivery.deliveryOtp();
        Long deliveryId = delivery.id();

        // Verify Automatic Dispatch assigned the driver!
        assertEquals(DeliveryStatus.DRIVER_ASSIGNED, delivery.status());
        assertNotNull(delivery.driverId(), "Driver should be automatically assigned by dispatch engine");

        // ─────────────────────────────────────────────────────────────
        // 6. DRIVER ACCEPTS JOB
        // ─────────────────────────────────────────────────────────────
        MvcResult acceptRes = mockMvc.perform(post("/api/v1/drivers/me/delivery/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andReturn();

        DeliveryResponse acceptedDelivery = objectMapper.readValue(
                acceptRes.getResponse().getContentAsString(),
                DeliveryResponse.class
        );
        assertEquals(DeliveryStatus.DRIVER_GOING_TO_PICKUP, acceptedDelivery.status());

        // ─────────────────────────────────────────────────────────────
        // 7. DRIVER PROGRESSION: ARRIVED PICKUP -> PICKED UP -> OUT FOR DELIVERY -> ARRIVED DROP
        // ─────────────────────────────────────────────────────────────
        mockMvc.perform(post("/api/v1/drivers/me/delivery/arrived-pickup")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/drivers/me/delivery/pickup")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/drivers/me/delivery/out-for-delivery")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/drivers/me/delivery/arrived-drop")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        // ─────────────────────────────────────────────────────────────
        // 8. VERIFY OTP REJECTION ON INVALID CODE
        // ─────────────────────────────────────────────────────────────
        DeliveryCompletionRequest badOtpReq = new DeliveryCompletionRequest(
                "0000", // wrong OTP
                "Alice Customer",
                "Delivered",
                null,
                37.7880,
                -122.3990
        );

        mockMvc.perform(post("/api/v1/drivers/me/delivery/complete")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badOtpReq)))
                .andExpect(status().isBadRequest());

        // ─────────────────────────────────────────────────────────────
        // 9. COMPLETE DELIVERY WITH VALID CUSTOMER OTP
        // ─────────────────────────────────────────────────────────────
        DeliveryCompletionRequest goodOtpReq = new DeliveryCompletionRequest(
                customerOtp,
                "Alice Customer",
                "Handed to customer in lobby",
                "https://cdn.example.com/pod-signature.png",
                37.7880,
                -122.3990
        );

        MvcResult completeRes = mockMvc.perform(post("/api/v1/drivers/me/delivery/complete")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodOtpReq)))
                .andExpect(status().isOk())
                .andReturn();

        DeliveryResponse completedDelivery = objectMapper.readValue(
                completeRes.getResponse().getContentAsString(),
                DeliveryResponse.class
        );

        assertEquals(DeliveryStatus.DELIVERED, completedDelivery.status());
        assertTrue(completedDelivery.otpVerified());
        assertNotNull(completedDelivery.actualDeliveryTime());

        // ─────────────────────────────────────────────────────────────
        // 10. VERIFY DRIVER FREED & PAYOUT RECORD CREATED
        // ─────────────────────────────────────────────────────────────
        Driver updatedDriver = driverRepo.findByEmail(driverEmail).orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, updatedDriver.getStatus(), "Driver should be freed back to AVAILABLE");

        // Verify Driver Earnings Summary
        MvcResult earningsRes = mockMvc.perform(get("/api/v1/drivers/me/earnings")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andReturn();

        DriverEarningsSummaryResponse earnings = objectMapper.readValue(
                earningsRes.getResponse().getContentAsString(),
                DriverEarningsSummaryResponse.class
        );

        assertTrue(earnings.totalEarnings() > 0.0, "Driver should have earned a payout");
        assertEquals(1, earnings.totalCompletedDeliveries());
        assertEquals(1, earnings.todayCompletedDeliveries());

        // Verify Driver Delivery History
        MvcResult historyRes = mockMvc.perform(get("/api/v1/drivers/me/deliveries")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andReturn();

        String historyJson = historyRes.getResponse().getContentAsString();
        assertTrue(historyJson.contains(externalOrderId), "Completed delivery should appear in driver history");
    }
}
