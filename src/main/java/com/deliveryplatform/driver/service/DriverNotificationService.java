package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.driver.entity.Driver;

public interface DriverNotificationService {

    void notifyJobAssigned(Driver driver, Delivery delivery);

    void notifyJobCancelled(Driver driver, Delivery delivery);

    void notifyGeneral(Long driverId, String title, String message);
}
