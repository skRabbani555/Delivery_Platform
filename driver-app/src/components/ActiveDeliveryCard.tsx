import React from 'react';
import {
  MapPin,
  Store,
  User,
  Phone,
  CheckCircle2,
  AlertTriangle,
  Play,
  Pause,
  ArrowRight,
  ShieldCheck,
  Package,
} from 'lucide-react';
import { Delivery, DeliveryStatus } from '../types/driver';

interface ActiveDeliveryCardProps {
  delivery: Delivery;
  onAction: (action: string) => void;
  isDrivingSimulated: boolean;
  onToggleSimulatedDrive: () => void;
  loading: boolean;
}

export const ActiveDeliveryCard: React.FC<ActiveDeliveryCardProps> = ({
  delivery,
  onAction,
  isDrivingSimulated,
  onToggleSimulatedDrive,
  loading,
}) => {
  const getStageInfo = (status: DeliveryStatus) => {
    switch (status) {
      case 'ASSIGNED':
      case 'ACCEPTED':
        return {
          title: 'Heading to Pickup',
          badgeClass: 'pickup',
          badgeLabel: 'Step 1 • En Route to Store',
          buttonText: 'I Have Arrived at Store',
          buttonClass: 'amber',
          action: 'ARRIVED_PICKUP',
        };
      case 'ARRIVED_AT_STORE':
        return {
          title: 'At Store: Collect Order',
          badgeClass: 'pickup',
          badgeLabel: 'Step 2 • Store Handoff',
          buttonText: 'Confirm Order Picked Up',
          buttonClass: 'indigo',
          action: 'PICKUP',
        };
      case 'PICKED_UP':
        return {
          title: 'Ready for Dispatch',
          badgeClass: 'drop',
          badgeLabel: 'Step 3 • Dispatched',
          buttonText: 'Start Route to Customer',
          buttonClass: 'indigo',
          action: 'OUT_FOR_DELIVERY',
        };
      case 'OUT_FOR_DELIVERY':
        return {
          title: 'Heading to Customer',
          badgeClass: 'drop',
          badgeLabel: 'Step 4 • Out for Delivery',
          buttonText: 'I Have Arrived at Drop',
          buttonClass: 'amber',
          action: 'ARRIVED_DROP',
        };
      case 'ARRIVED_AT_CUSTOMER':
        return {
          title: 'At Customer: Proof of Delivery',
          badgeClass: 'drop',
          badgeLabel: 'Step 5 • Customer Verification',
          buttonText: 'Verify OTP & Complete Delivery',
          buttonClass: 'emerald',
          action: 'OPEN_OTP_MODAL',
        };
      default:
        return {
          title: 'Active Delivery',
          badgeClass: 'pickup',
          badgeLabel: status,
          buttonText: 'Next Step',
          buttonClass: 'indigo',
          action: 'NEXT',
        };
    }
  };

  const stage = getStageInfo(delivery.status);

  return (
    <div className="active-delivery-card" id="active-delivery-sheet">
      {/* Top Header */}
      <div className="delivery-stage-bar">
        <span className={`stage-badge ${stage.badgeClass}`}>
          <Package size={14} />
          {stage.badgeLabel}
        </span>
        <div className="order-payout-chip" id="delivery-payout">
          +${delivery.driverPayout.toFixed(2)}
        </div>
      </div>

      {/* Target Address details */}
      <div className="delivery-addresses">
        {/* Pickup address */}
        <div
          className="address-row"
          style={{
            opacity:
              delivery.status === 'OUT_FOR_DELIVERY' || delivery.status === 'ARRIVED_AT_CUSTOMER'
                ? 0.45
                : 1,
          }}
        >
          <div className="address-dot pickup" />
          <div className="address-info">
            <span className="address-title">Pickup • Store</span>
            <span className="address-text">{delivery.pickupAddress}</span>
          </div>
        </div>

        {/* Drop address */}
        <div
          className="address-row"
          style={{
            opacity:
              delivery.status === 'ASSIGNED' ||
              delivery.status === 'ACCEPTED' ||
              delivery.status === 'ARRIVED_AT_STORE'
                ? 0.65
                : 1,
          }}
        >
          <div className="address-dot drop" />
          <div className="address-info">
            <span className="address-title">Dropoff • Customer</span>
            <span className="address-text">{delivery.dropAddress}</span>
          </div>
        </div>
      </div>

      {/* Order Context / Items info */}
      {delivery.items && delivery.items.length > 0 && (
        <div
          style={{
            fontSize: '12px',
            color: 'var(--text-secondary)',
            background: 'rgba(255, 255, 255, 0.03)',
            padding: '8px 12px',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <strong style={{ color: 'var(--text-primary)' }}>Order items: </strong>
          {delivery.items.join(', ')}
        </div>
      )}

      {/* Contact Bar */}
      <div className="contact-quick-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <User size={15} color="var(--primary)" />
          <span style={{ fontWeight: 600 }}>{delivery.customerName}</span>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={onToggleSimulatedDrive}
            className="quick-action-btn"
            id="simulate-drive-btn"
            title={isDrivingSimulated ? 'Pause GPS Drive' : 'Auto-Drive Simulation'}
            style={{
              borderColor: isDrivingSimulated ? 'var(--emerald)' : undefined,
              color: isDrivingSimulated ? 'var(--emerald)' : undefined,
            }}
          >
            {isDrivingSimulated ? <Pause size={13} /> : <Play size={13} />}
            {isDrivingSimulated ? 'Driving' : 'Simulate Drive'}
          </button>
          <a
            href={`tel:${delivery.customerPhone}`}
            className="quick-action-btn"
            id="call-customer-btn"
          >
            <Phone size={13} />
            Call
          </a>
        </div>
      </div>

      {/* Primary Action Button */}
      <button
        className={`action-cta-btn ${stage.buttonClass}`}
        id="delivery-main-action-btn"
        disabled={loading}
        onClick={() => onAction(stage.action)}
      >
        {loading ? (
          'Updating...'
        ) : (
          <>
            {stage.buttonText}
            <ArrowRight size={18} />
          </>
        )}
      </button>

      {/* Auxiliary / Emergency Action */}
      {delivery.status === 'ARRIVED_AT_CUSTOMER' && (
        <button
          onClick={() => onAction('REPORT_FAILURE')}
          id="fail-delivery-trigger-btn"
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--rose)',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
            cursor: 'pointer',
            padding: '4px',
          }}
        >
          <AlertTriangle size={13} />
          Can't Deliver? Report Issue / Unreachable
        </button>
      )}
    </div>
  );
};
