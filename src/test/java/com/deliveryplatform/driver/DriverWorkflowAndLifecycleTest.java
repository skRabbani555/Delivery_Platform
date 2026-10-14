package com.deliveryplatform.driver;

import com.deliveryplatform.delivery.dto.DeliveryCompletionRequest;
import com.deliveryplatform.delivery.dto.DeliveryFailureRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryEvent;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.entity.DeliveryType;
import com.deliveryplatform.delivery.repo.DeliveryEventRepo;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.delivery.service.DeliveryService;
import com.deliveryplatform.dispatch.service.DispatchService;
import com.deliveryplatform.driver.dto.DriverEarningsSummaryResponse;
import com.deliveryplatform.driver.dto.DriverResponse;
import com.deliveryplatform.driver.dto.UpdateLocationRequest;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverPayout;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.entity.PayoutStatus;
import com.deliveryplatform.driver.repo.DriverPayoutRepo;
import com.deliveryplatform.driver.repo.DriverRepo;
import com.deliveryplatform.driver.service.*;
import com.deliveryplatform.integration.service.WebhookService;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import com.deliveryplatform.pricing.service.PricingService;
import com.deliveryplatform.store.entity.Store;
import com.deliveryplatform.store.repo.StoreRepo;
import com.deliveryplatform.tracking.service.DriverLocationWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverWorkflowAndLifecycleTest {

    @Mock
    private DeliveryRepo deliveryRepo;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private StoreRepo storeRepo;
    @Mock
    private DispatchService dispatchService;
    @Mock
    private DeliveryEventRepo deliveryEventRepo;
    @Mock
    private WebhookService webhookService;
    @Mock
    private PricingService pricingService;
    @Mock
    private DriverRepo driverRepo;
    @Mock
    private DriverPayoutRepo driverPayoutRepo;
    @Mock
    private DriverLocationService driverLocationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DriverNotificationService driverNotificationService;

    private DriverPayoutService driverPayoutService;
    private DeliveryService deliveryService;
    private DriverService driverService;
    private DriverDeliveryService driverDeliveryService;

    private Driver sampleDriver;
    private Merchant sampleMerchant;
    private Store sampleStore;
    private Delivery sampleDelivery;

    @BeforeEach
    void setUp() {
        driverPayoutService = new DriverPayoutService(driverPayoutRepo);

        deliveryService = new DeliveryService(
                deliveryRepo,
                merchantRepository,
                storeRepo,
                dispatchService,
                deliveryEventRepo,
                webhookService,
                pricingService,
                driverRepo,
                driverPayoutService
        );

        driverService = new DriverService(
                driverRepo,
                driverLocationService,
                passwordEncoder,
                deliveryRepo,
                dispatchService
        );

        driverDeliveryService = new DriverDeliveryService(
                deliveryRepo,
                deliveryService
        );

        sampleDriver = new Driver();
        sampleDriver.setName("Alex Rider");
        sampleDriver.setEmail("alex@delivery.com");
        sampleDriver.setPhone("1234567890");
        sampleDriver.setStatus(DriverStatus.BUSY);
        sampleDriver.setActive(true);
        sampleDriver.setLatitude(37.7749);
        sampleDriver.setLongitude(-122.4194);

        // reflection or fake ID for test
        setEntityId(sampleDriver, 101L);

        sampleMerchant = Merchant.builder()
                .id(1L)
                .businessName("Acme Burger")
                .email("acme@burger.com")
                .build();

        sampleStore = Store.builder()
                .id(2L)
                .merchant(sampleMerchant)
                .name("Acme Downtown")
                .build();

        sampleDelivery = Delivery.builder()
                .id(501L)
                .merchant(sampleMerchant)
                .store(sampleStore)
                .driver(sampleDriver)
                .externalOrderId("EXT-12345")
                .pickupAddress("100 Main St")
                .dropAddress("200 Elm St")
                .customerName("Jane Doe")
                .customerPhone("9876543210")
                .deliveryType(DeliveryType.INSTANT)
                .status(DeliveryStatus.ARRIVED_AT_DROP)
                .deliveryFee(25.0)
                .driverPayout(20.0)
                .platformFee(5.0)
                .deliveryOtp("7842")
                .otpVerified(false)
                .build();
    }

    private void setEntityId(Object target, Long id) {
        try {
            var field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Driver Availability Rules")
    class DriverAvailabilityTests {

        @Test
        @DisplayName("Should prevent driver from switching to AVAILABLE when assigned to an active delivery")
        void shouldPreventAvailableWhenActiveDeliveryExists() {
            when(driverRepo.findById(101L)).thenReturn(Optional.of(sampleDriver));
            when(dispatchService.hasActiveDelivery(101L)).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    driverService.updateStatus(101L, DriverStatus.AVAILABLE));

            assertTrue(ex.getMessage().contains("Cannot switch status to AVAILABLE while assigned to an active delivery"));
            verify(driverRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should allow driver to switch to AVAILABLE when no active delivery exists and trigger dispatch sweep")
        void shouldAllowAvailableWhenIdle() {
            when(driverRepo.findById(101L)).thenReturn(Optional.of(sampleDriver));
            when(dispatchService.hasActiveDelivery(101L)).thenReturn(false);
            when(driverRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DriverResponse resp = driverService.updateStatus(101L, DriverStatus.AVAILABLE);

            assertEquals(DriverStatus.AVAILABLE, resp.status());
            verify(dispatchService).triggerPendingDispatchAsync();
        }

        @Test
        @DisplayName("Should prevent going OFFLINE when in-transit for an active delivery")
        void shouldPreventOfflineWhenInTransit() {
            when(driverRepo.findById(101L)).thenReturn(Optional.of(sampleDriver));
            when(deliveryRepo.findByDriverIdAndStatusNotIn(eq(101L), anyList()))
                    .thenReturn(List.of(sampleDelivery));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    driverService.updateStatus(101L, DriverStatus.OFFLINE));

            assertTrue(ex.getMessage().contains("Cannot go OFFLINE while in-transit"));
            verify(driverRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should allow going OFFLINE when idle")
        void shouldAllowOfflineWhenIdle() {
            when(driverRepo.findById(101L)).thenReturn(Optional.of(sampleDriver));
            when(deliveryRepo.findByDriverIdAndStatusNotIn(eq(101L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(deliveryRepo.findByDriverIdAndStatus(101L, DeliveryStatus.DRIVER_ASSIGNED))
                    .thenReturn(Collections.emptyList());
            when(driverRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DriverResponse resp = driverService.updateStatus(101L, DriverStatus.OFFLINE);

            assertEquals(DriverStatus.OFFLINE, resp.status());
        }
    }

    @Nested
    @DisplayName("Proof of Delivery (OTP) & Delivery Completion Verification")
    class ProofOfDeliveryTests {

        @Test
        @DisplayName("Should verify valid OTP and complete delivery successfully")
        void shouldVerifyValidOtpAndCompleteDelivery() {
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(driverRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(driverPayoutRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DeliveryCompletionRequest request = new DeliveryCompletionRequest(
                    "7842",
                    "Jane Doe",
                    "Package handed over at apartment door",
                    "https://cdn.example.com/pod.jpg",
                    37.7749,
                    -122.4194
            );

            DeliveryResponse response = deliveryService.verifyAndCompleteDelivery(501L, 101L, request);

            assertEquals(DeliveryStatus.DELIVERED, response.status());
            assertTrue(response.otpVerified());
            assertEquals("7842", response.deliveryOtp());
            assertEquals("Package handed over at apartment door", response.proofOfDeliveryNotes());
            assertEquals(DriverStatus.AVAILABLE, sampleDriver.getStatus());

            // Payout record recorded
            verify(driverPayoutRepo).save(any(DriverPayout.class));
            // Event logged
            verify(deliveryEventRepo).save(any(DeliveryEvent.class));
            // Webhook fired
            verify(webhookService).sendDeliveryStatusWebhook(any(Delivery.class));
            // Sweep triggered
            verify(dispatchService).triggerPendingDispatchAsync();
        }

        @Test
        @DisplayName("Should reject invalid OTP during delivery completion")
        void shouldRejectInvalidOtp() {
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));

            DeliveryCompletionRequest request = new DeliveryCompletionRequest(
                    "9999", // wrong OTP
                    "Jane Doe",
                    "Notes",
                    null,
                    null,
                    null
            );

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    deliveryService.verifyAndCompleteDelivery(501L, 101L, request));

            assertTrue(ex.getMessage().contains("Invalid delivery verification OTP"));
            verify(driverPayoutRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should reject completion if delivery is not at drop location")
        void shouldRejectCompletionIfNotAtDropLocation() {
            sampleDelivery.setStatus(DeliveryStatus.DRIVER_GOING_TO_PICKUP);
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));

            DeliveryCompletionRequest request = new DeliveryCompletionRequest(
                    "7842",
                    "Jane Doe",
                    null,
                    null,
                    null,
                    null
            );

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    deliveryService.verifyAndCompleteDelivery(501L, 101L, request));

            assertTrue(ex.getMessage().contains("Delivery must be at drop location before completion"));
        }
    }

    @Nested
    @DisplayName("Delivery Failure & Cancellation Hardening")
    class DeliveryFailureAndCancellationTests {

        @Test
        @DisplayName("Should successfully record delivery failure at drop location")
        void shouldRecordDeliveryFailure() {
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(driverRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DeliveryFailureRequest request = new DeliveryFailureRequest(
                    "Customer unavailable",
                    "Called customer 3 times, no answer at door bell",
                    37.7749,
                    -122.4194
            );

            DeliveryResponse response = deliveryService.failDelivery(501L, 101L, request);

            assertEquals(DeliveryStatus.FAILED, response.status());
            assertTrue(response.failureReason().contains("Customer unavailable"));
            assertEquals(DriverStatus.AVAILABLE, sampleDriver.getStatus());
            verify(webhookService).sendDeliveryStatusWebhook(any());
            verify(dispatchService).triggerPendingDispatchAsync();
        }

        @Test
        @DisplayName("Should prevent cancelling an in-transit delivery after pickup")
        void shouldPreventCancelAfterPickup() {
            sampleDelivery.setStatus(DeliveryStatus.PICKED_UP);
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    deliveryService.cancelDelivery(501L, "DRIVER", 101L));

            assertTrue(ex.getMessage().contains("Package has already been picked up. In-transit delivery cannot be cancelled"));
        }

        @Test
        @DisplayName("Should disallow direct DELIVERED transition via updateStatus without OTP")
        void shouldDisallowDirectDeliveredViaUpdateStatus() {
            sampleDelivery.setStatus(DeliveryStatus.ARRIVED_AT_DROP);
            when(deliveryRepo.findById(501L)).thenReturn(Optional.of(sampleDelivery));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    deliveryService.updateStatus(501L, DeliveryStatus.DELIVERED, "DRIVER", 101L));

            assertTrue(ex.getMessage().contains("Please call the /complete endpoint with customer verification OTP"));
        }
    }

    @Nested
    @DisplayName("Driver Earnings and Payout Service")
    class DriverPayoutServiceTests {

        @Test
        @DisplayName("Should create payout record correctly and calculate summary")
        void shouldCalculateEarningsSummary() {
            DriverPayout payout1 = DriverPayout.builder()
                    .id(1L)
                    .driver(sampleDriver)
                    .delivery(sampleDelivery)
                    .amount(20.0)
                    .deliveryFee(25.0)
                    .platformFee(5.0)
                    .status(PayoutStatus.PENDING)
                    .earnedAt(LocalDateTime.now())
                    .referenceNumber("PAYOUT-501-12345")
                    .build();

            when(driverPayoutRepo.sumEarningsByDriverId(101L)).thenReturn(85.50);
            when(driverPayoutRepo.sumEarningsByDriverIdAndEarnedAtAfter(eq(101L), any())).thenReturn(20.00);
            when(driverPayoutRepo.countByDriverId(101L)).thenReturn(5L);
            when(driverPayoutRepo.countByDriverIdAndEarnedAtAfter(eq(101L), any())).thenReturn(1L);
            when(driverPayoutRepo.findByDriverIdOrderByEarnedAtDesc(101L)).thenReturn(List.of(payout1));

            DriverEarningsSummaryResponse summary = driverPayoutService.getDriverEarningsSummary(101L);

            assertEquals(101L, summary.driverId());
            assertEquals(85.50, summary.totalEarnings());
            assertEquals(20.00, summary.todayEarnings());
            assertEquals(5, summary.totalCompletedDeliveries());
            assertEquals(1, summary.todayCompletedDeliveries());
            assertEquals(1, summary.recentPayouts().size());
            assertEquals(20.0, summary.recentPayouts().get(0).amount());
        }
    }

    @Nested
    @DisplayName("Location Handling and Geofencing")
    class LocationHandlingTests {

        @Test
        @DisplayName("Haversine distance calculation and geofencing verification")
        void shouldCalculateHaversineDistanceAccurately() {
            // Distance between SF City Hall and Ferry Building is ~2.0 km (~2000m)
            double lat1 = 37.7793;
            double lon1 = -122.4192;
            double lat2 = 37.7955;
            double lon2 = -122.3937;

            double distanceMeters = DriverLocationService.calculateDistanceMeters(lat1, lon1, lat2, lon2);

            assertTrue(distanceMeters > 2000 && distanceMeters < 3000,
                    "Distance should be between 2km and 3km, got: " + distanceMeters);

            // Within 5km radius -> true
            assertTrue(DriverLocationService.isWithinRadius(lat1, lon1, lat2, lon2, 3500.0));
            // Within 1km radius -> false
            assertFalse(DriverLocationService.isWithinRadius(lat1, lon1, lat2, lon2, 1000.0));
        }

        @Test
        @DisplayName("Should reject invalid coordinates")
        void shouldRejectInvalidCoordinates() {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            DriverLocationWebSocketService ws = mock(DriverLocationWebSocketService.class);
            DriverLocationService service = new DriverLocationService(redis, ws, deliveryRepo);

            UpdateLocationRequest invalidLat = new UpdateLocationRequest(100.0, -122.0);
            assertThrows(IllegalArgumentException.class, () ->
                    service.updateLocation(101L, invalidLat));

            UpdateLocationRequest invalidLon = new UpdateLocationRequest(37.0, -200.0);
            assertThrows(IllegalArgumentException.class, () ->
                    service.updateLocation(101L, invalidLon));
        }
    }
}
