export type DriverStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE';

export type DeliveryStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'ARRIVED_AT_STORE'
  | 'PICKED_UP'
  | 'OUT_FOR_DELIVERY'
  | 'ARRIVED_AT_CUSTOMER'
  | 'DELIVERED'
  | 'FAILED'
  | 'CANCELLED';

export type DeliveryType = 'ON_DEMAND' | 'SCHEDULED' | 'SAME_DAY';

export type PayoutStatus = 'PENDING' | 'PROCESSED' | 'FAILED';

export interface Driver {
  id: number;
  name: string;
  email: string;
  phone: string;
  status: DriverStatus;
  latitude: number;
  longitude: number;
  active: boolean;
  vehicleType?: string;
  vehiclePlate?: string;
  rating?: number;
}

export interface Delivery {
  id: number;
  merchantId: number;
  storeId?: number;
  driverId?: number;
  externalOrderId: string;
  pickupAddress: string;
  dropAddress: string;
  customerName: string;
  customerPhone: string;
  packageWeight?: number;
  deliveryType: DeliveryType;
  status: DeliveryStatus;
  deliveryFee?: number;
  driverPayout: number;
  platformFee?: number;
  estimatedEtaMinutes?: number;
  assignedAt?: string;
  deliveryOtp?: string;
  otpVerified?: boolean;
  proofOfDeliveryNotes?: string;
  proofOfDeliveryImageUrl?: string;
  recipientName?: string;
  actualPickupTime?: string;
  actualDeliveryTime?: string;
  failureReason?: string;
  pickupLat?: number;
  pickupLng?: number;
  dropLat?: number;
  dropLng?: number;
  items?: string[];
  notes?: string;
}

export interface DriverEarningsSummary {
  driverId: number;
  totalEarnings: number;
  todayEarnings: number;
  totalCompletedDeliveries: number;
  todayCompletedDeliveries: number;
  recentPayouts: DriverPayout[];
}

export interface DriverPayout {
  id: number;
  driverId: number;
  deliveryId: number;
  amount: number;
  status: PayoutStatus;
  createdAt: string;
}

export interface LocationCoordinates {
  latitude: number;
  longitude: number;
  heading?: number;
  speed?: number;
  accuracy?: number;
}

export interface DeliveryCompletionPayload {
  otp: string;
  recipientName?: string;
  notes?: string;
  imageUrl?: string;
  dropLatitude?: number;
  dropLongitude?: number;
}

export interface DeliveryFailurePayload {
  reason: string;
  notes?: string;
  latitude?: number;
  longitude?: number;
}
