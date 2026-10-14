import React, { useEffect, useState } from 'react';
import { Clock, Navigation, CheckCircle2, XCircle, DollarSign, Store, MapPin } from 'lucide-react';
import { Delivery } from '../types/driver';

interface IncomingOfferModalProps {
  delivery: Delivery;
  onAccept: () => void;
  onDecline: () => void;
  timeoutSeconds?: number;
}

export const IncomingOfferModal: React.FC<IncomingOfferModalProps> = ({
  delivery,
  onAccept,
  onDecline,
  timeoutSeconds = 45,
}) => {
  const [secondsRemaining, setSecondsRemaining] = useState(timeoutSeconds);

  // Play subtle web audio beep notification
  useEffect(() => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
      osc.frequency.setValueAtTime(880, audioCtx.currentTime + 0.15); // A5
      gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.45);
    } catch {
      // Audio autoplay policy fallback
    }
  }, []);

  // Countdown timer
  useEffect(() => {
    if (secondsRemaining <= 0) {
      onDecline();
      return;
    }
    const timer = setInterval(() => {
      setSecondsRemaining((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [secondsRemaining, onDecline]);

  return (
    <div className="modal-backdrop">
      <div className="offer-modal" id="incoming-offer-dialog">
        {/* Top Header */}
        <div className="offer-top">
          <div className="offer-payout-box">
            <span className="offer-payout-amount" id="offer-payout-amount">
              ${delivery.driverPayout.toFixed(2)}
            </span>
            <span className="offer-payout-label">Guaranteed Earnings</span>
          </div>

          <div className="countdown-badge" id="offer-countdown-timer">
            <Clock size={16} />
            <span>{secondsRemaining}s</span>
          </div>
        </div>

        {/* Quick Route Meta */}
        <div className="offer-meta-chips">
          <div className="meta-chip">
            <span className="meta-chip-val">
              {delivery.estimatedEtaMinutes || 18} min
            </span>
            <span className="meta-chip-lbl">Estimated Trip</span>
          </div>
          <div className="meta-chip">
            <span className="meta-chip-val">3.4 km</span>
            <span className="meta-chip-lbl">Total Distance</span>
          </div>
          <div className="meta-chip">
            <span className="meta-chip-val">{delivery.packageWeight || 1.8} kg</span>
            <span className="meta-chip-lbl">Package Wt.</span>
          </div>
        </div>

        {/* Addresses */}
        <div className="delivery-addresses">
          <div className="address-row">
            <div className="address-dot pickup" />
            <div className="address-info">
              <span className="address-title">Pickup</span>
              <span className="address-text">{delivery.pickupAddress}</span>
            </div>
          </div>

          <div className="address-row">
            <div className="address-dot drop" />
            <div className="address-info">
              <span className="address-title">Dropoff</span>
              <span className="address-text">{delivery.dropAddress}</span>
            </div>
          </div>
        </div>

        {/* Order Items preview */}
        {delivery.items && (
          <div
            style={{
              fontSize: '12px',
              color: 'var(--text-secondary)',
              background: 'rgba(255, 255, 255, 0.03)',
              padding: '8px 12px',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            {delivery.items.join(' • ')}
          </div>
        )}

        {/* Actions */}
        <div className="offer-actions">
          <button
            className="offer-decline-btn"
            id="offer-decline-btn"
            onClick={onDecline}
          >
            Decline
          </button>
          <button
            className="offer-accept-btn"
            id="offer-accept-btn"
            onClick={onAccept}
          >
            Accept Delivery
          </button>
        </div>
      </div>
    </div>
  );
};
