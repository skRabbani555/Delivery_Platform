import React, { useState, useEffect } from 'react';
import confetti from 'canvas-confetti';
import {
  Navigation,
  DollarSign,
  Clock,
  User,
  Radio,
  Power,
  Zap,
  CheckCircle,
} from 'lucide-react';

import {
  Driver,
  Delivery,
  DriverEarningsSummary,
  DriverStatus,
  DeliveryCompletionPayload,
  DeliveryFailurePayload,
} from './types/driver';
import {
  INITIAL_DRIVER,
  SAMPLE_DELIVERY_OFFER,
  INITIAL_EARNINGS_SUMMARY,
  INITIAL_DELIVERY_HISTORY,
} from './services/mockData';
import { api } from './services/api';
import { wsService } from './services/websocket';

import { Navbar } from './components/Navbar';
import { MapComponent } from './components/MapComponent';
import { ActiveDeliveryCard } from './components/ActiveDeliveryCard';
import { IncomingOfferModal } from './components/IncomingOfferModal';
import { OtpVerificationModal } from './components/OtpVerificationModal';
import { FailureReportModal } from './components/FailureReportModal';
import { EarningsView } from './components/EarningsView';
import { HistoryView } from './components/HistoryView';
import { ProfileView } from './components/ProfileView';
import { LoginModal } from './components/LoginModal';

type ActiveTab = 'MAP' | 'EARNINGS' | 'HISTORY' | 'PROFILE';

export const App: React.FC = () => {
  // Navigation & View State
  const [activeTab, setActiveTab] = useState<ActiveTab>('MAP');

  // Driver & Connectivity State
  const [driver, setDriver] = useState<Driver>(INITIAL_DRIVER);
  const [isLiveBackend, setIsLiveBackend] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  // Delivery & Offer State
  const [activeDelivery, setActiveDelivery] = useState<Delivery | null>(null);
  const [incomingOffer, setIncomingOffer] = useState<Delivery | null>(null);
  const [isDrivingSimulated, setIsDrivingSimulated] = useState<boolean>(false);

  // Modals
  const [showOtpModal, setShowOtpModal] = useState<boolean>(false);
  const [showFailureModal, setShowFailureModal] = useState<boolean>(false);
  const [showLoginModal, setShowLoginModal] = useState<boolean>(false);

  // Financial & History Records
  const [earningsSummary, setEarningsSummary] = useState<DriverEarningsSummary>(
    INITIAL_EARNINGS_SUMMARY
  );
  const [deliveryHistory, setDeliveryHistory] = useState<Delivery[]>(
    INITIAL_DELIVERY_HISTORY
  );

  // Check backend availability on mount
  useEffect(() => {
    const checkBackend = async () => {
      try {
        const profile = await api.getProfile();
        setDriver(profile);
        setIsLiveBackend(true);
        // Connect WebSocket STOMP
        wsService.connect(api.getToken());
      } catch {
        // Backend not currently responding or unauthorized; default gracefully to sandbox mode
        setIsLiveBackend(false);
      }
    };
    checkBackend();
  }, []);

  // Poll active delivery if connected to live backend
  useEffect(() => {
    if (!isLiveBackend || driver.status === 'OFFLINE') return;

    const interval = setInterval(async () => {
      try {
        const active = await api.getActiveDelivery();
        if (active && (!activeDelivery || activeDelivery.id !== active.id)) {
          if (active.status === 'ASSIGNED') {
            setIncomingOffer(active);
          } else {
            setActiveDelivery(active);
          }
        }
      } catch {
        // Silent poll
      }
    }, 4000);

    return () => clearInterval(interval);
  }, [isLiveBackend, driver.status, activeDelivery]);

  // Handle Online/Offline Shift Toggle
  const handleToggleShift = async () => {
    const nextStatus: DriverStatus = driver.status === 'OFFLINE' ? 'AVAILABLE' : 'OFFLINE';

    if (isLiveBackend) {
      try {
        setLoading(true);
        const updated = await api.updateStatus(nextStatus);
        setDriver(updated);
      } catch (err: any) {
        alert(`Status update failed: ${err.message}. Switching to sandbox.`);
        setDriver((prev) => ({ ...prev, status: nextStatus }));
      } finally {
        setLoading(false);
      }
    } else {
      setDriver((prev) => ({ ...prev, status: nextStatus }));

      // Demo experience: When driver goes online, automatically dispatch an offer in 3.5 seconds!
      if (nextStatus === 'AVAILABLE' && !activeDelivery && !incomingOffer) {
        setTimeout(() => {
          setIncomingOffer(SAMPLE_DELIVERY_OFFER);
        }, 3500);
      }
    }
  };

  // Handle Updating Coordinates
  const handleUpdateCoordinates = async (lat: number, lng: number) => {
    setDriver((prev) => ({ ...prev, latitude: lat, longitude: lng }));

    if (isLiveBackend) {
      try {
        await api.updateLocation({ latitude: lat, longitude: lng });
      } catch {
        // Telemetry error handled silently
      }
    }
  };

  // Accept incoming offer
  const handleAcceptOffer = async () => {
    if (!incomingOffer) return;
    setLoading(true);

    try {
      if (isLiveBackend) {
        const accepted = await api.acceptDelivery();
        setActiveDelivery(accepted);
      } else {
        const accepted: Delivery = { ...incomingOffer, status: 'ACCEPTED' };
        setActiveDelivery(accepted);
        setDriver((prev) => ({ ...prev, status: 'BUSY' }));
      }
      setIncomingOffer(null);
      setActiveTab('MAP');
    } catch (err: any) {
      alert(`Accept failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Decline incoming offer
  const handleDeclineOffer = async () => {
    if (isLiveBackend) {
      try {
        await api.rejectDelivery();
      } catch (err) {
        console.error(err);
      }
    }
    setIncomingOffer(null);
  };

  // Transition through Delivery Lifecycle Actions
  const handleDeliveryAction = async (action: string) => {
    if (!activeDelivery) return;

    if (action === 'OPEN_OTP_MODAL') {
      setShowOtpModal(true);
      return;
    }

    if (action === 'REPORT_FAILURE') {
      setShowFailureModal(true);
      return;
    }

    setLoading(true);
    try {
      if (action === 'ARRIVED_PICKUP') {
        if (isLiveBackend) {
          const res = await api.arrivedAtPickup();
          setActiveDelivery(res);
        } else {
          setActiveDelivery((prev) => (prev ? { ...prev, status: 'ARRIVED_AT_STORE' } : null));
        }
      } else if (action === 'PICKUP') {
        if (isLiveBackend) {
          const res = await api.pickupOrder();
          setActiveDelivery(res);
        } else {
          setActiveDelivery((prev) => (prev ? { ...prev, status: 'PICKED_UP' } : null));
        }
      } else if (action === 'OUT_FOR_DELIVERY') {
        if (isLiveBackend) {
          const res = await api.outForDelivery();
          setActiveDelivery(res);
        } else {
          setActiveDelivery((prev) => (prev ? { ...prev, status: 'OUT_FOR_DELIVERY' } : null));
        }
      } else if (action === 'ARRIVED_DROP') {
        if (isLiveBackend) {
          const res = await api.arrivedAtDrop();
          setActiveDelivery(res);
        } else {
          setActiveDelivery((prev) => (prev ? { ...prev, status: 'ARRIVED_AT_CUSTOMER' } : null));
        }
      }
    } catch (err: any) {
      alert(`Action error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Complete Delivery with OTP verification
  const handleVerifyOtpAndComplete = async (payload: DeliveryCompletionPayload) => {
    if (!activeDelivery) return;
    setLoading(true);

    try {
      let completedDelivery: Delivery;

      if (isLiveBackend) {
        completedDelivery = await api.completeDelivery(payload);
      } else {
        completedDelivery = {
          ...activeDelivery,
          status: 'DELIVERED',
          otpVerified: true,
          recipientName: payload.recipientName || activeDelivery.customerName,
          actualDeliveryTime: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        };
      }

      // Trigger Confetti Celebration! 🎉
      confetti({
        particleCount: 120,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#10b981', '#6366f1', '#38bdf8', '#f59e0b'],
      });

      // Update Financials & History
      const payoutAmount = completedDelivery.driverPayout;
      setEarningsSummary((prev) => ({
        ...prev,
        todayEarnings: prev.todayEarnings + payoutAmount,
        totalEarnings: prev.totalEarnings + payoutAmount,
        todayCompletedDeliveries: prev.todayCompletedDeliveries + 1,
        totalCompletedDeliveries: prev.totalCompletedDeliveries + 1,
        recentPayouts: [
          {
            id: Date.now(),
            driverId: driver.id,
            deliveryId: completedDelivery.id,
            amount: payoutAmount,
            status: 'PROCESSED',
            createdAt: new Date().toISOString(),
          },
          ...prev.recentPayouts,
        ],
      }));

      setDeliveryHistory((prev) => [completedDelivery, ...prev]);

      // Reset Active delivery
      setActiveDelivery(null);
      setShowOtpModal(false);
      setIsDrivingSimulated(false);
      setDriver((prev) => ({ ...prev, status: 'AVAILABLE' }));
    } catch (err: any) {
      alert(`Completion failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Handle Failure
  const handleReportFailure = async (payload: DeliveryFailurePayload) => {
    if (!activeDelivery) return;
    setLoading(true);

    try {
      let failedDelivery: Delivery;

      if (isLiveBackend) {
        failedDelivery = await api.failDelivery(payload);
      } else {
        failedDelivery = {
          ...activeDelivery,
          status: 'FAILED',
          failureReason: payload.reason,
          actualDeliveryTime: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        };
      }

      setDeliveryHistory((prev) => [failedDelivery, ...prev]);
      setActiveDelivery(null);
      setShowFailureModal(false);
      setIsDrivingSimulated(false);
      setDriver((prev) => ({ ...prev, status: 'AVAILABLE' }));
    } catch (err: any) {
      alert(`Failure reporting error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container" id="swiftpulse-driver-app">
      {/* Top Header */}
      <Navbar
        driver={driver}
        onToggleShift={handleToggleShift}
        onProfileClick={() => setActiveTab('PROFILE')}
        isLiveBackend={isLiveBackend}
        todayEarnings={earningsSummary.todayEarnings}
      />

      {/* Main Viewport */}
      <main className="main-viewport">
        {activeTab === 'MAP' && (
          <>
            {/* Interactive Leaflet Map */}
            <MapComponent
              driver={driver}
              activeDelivery={activeDelivery}
              onUpdateCoordinates={handleUpdateCoordinates}
              isDrivingSimulated={isDrivingSimulated}
            />

            {/* Offline Card */}
            {driver.status === 'OFFLINE' && !activeDelivery && (
              <div className="offline-card" id="driver-offline-card">
                <div className="offline-icon">
                  <Power size={26} />
                </div>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 700, fontFamily: 'var(--font-heading)' }}>
                    You are Currently Offline
                  </h3>
                  <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                    Go online to receive real-time delivery orders and earn payouts.
                  </p>
                </div>
                <button
                  onClick={handleToggleShift}
                  className="action-cta-btn emerald"
                  id="go-online-cta-btn"
                >
                  Go Online Now
                </button>
              </div>
            )}

            {/* Radar Searching Animation (Online, Waiting for Orders) */}
            {driver.status === 'AVAILABLE' && !activeDelivery && !incomingOffer && (
              <div className="radar-overlay" id="radar-searching-overlay">
                <div className="radar-header">
                  <div className="radar-scanner">
                    <span className="radar-ring" />
                    <span className="radar-ring" />
                    <span className="radar-ring" />
                    <Radio size={22} color="var(--emerald)" />
                  </div>
                  <div className="radar-text">
                    <h3>Searching for Orders</h3>
                    <p>High demand in your zone • Stay active</p>
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '12px',
                    color: 'var(--text-secondary)',
                    background: 'rgba(255, 255, 255, 0.04)',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <span>Today's Total: <strong>${earningsSummary.todayEarnings.toFixed(2)}</strong></span>
                  <button
                    onClick={() => setIncomingOffer(SAMPLE_DELIVERY_OFFER)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--amber)',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    + Trigger Test Order
                  </button>
                </div>
              </div>
            )}

            {/* Active Delivery Workflow Card */}
            {activeDelivery && (
              <ActiveDeliveryCard
                delivery={activeDelivery}
                onAction={handleDeliveryAction}
                isDrivingSimulated={isDrivingSimulated}
                onToggleSimulatedDrive={() => setIsDrivingSimulated(!isDrivingSimulated)}
                loading={loading}
              />
            )}
          </>
        )}

        {/* Earnings View Tab */}
        {activeTab === 'EARNINGS' && (
          <EarningsView summary={earningsSummary} />
        )}

        {/* History View Tab */}
        {activeTab === 'HISTORY' && (
          <HistoryView deliveries={deliveryHistory} />
        )}

        {/* Profile & Settings Tab */}
        {activeTab === 'PROFILE' && (
          <ProfileView
            driver={driver}
            onUpdateStatus={(st) => setDriver((p) => ({ ...p, status: st }))}
            isLiveBackend={isLiveBackend}
            onToggleBackendMode={() => setIsLiveBackend(!isLiveBackend)}
            onOpenLoginModal={() => setShowLoginModal(true)}
            onSimulateOrder={() => {
              setIncomingOffer(SAMPLE_DELIVERY_OFFER);
              setActiveTab('MAP');
            }}
            onLogout={() => {
              api.clearAuth();
              setDriver(INITIAL_DRIVER);
              setIsLiveBackend(false);
            }}
          />
        )}
      </main>

      {/* Bottom Navigation */}
      <nav className="bottom-nav" id="app-bottom-nav">
        <button
          className={`nav-item ${activeTab === 'MAP' ? 'active' : ''}`}
          onClick={() => setActiveTab('MAP')}
          id="tab-btn-map"
        >
          <Navigation size={20} />
          <span>Deliveries</span>
        </button>

        <button
          className={`nav-item ${activeTab === 'EARNINGS' ? 'active' : ''}`}
          onClick={() => setActiveTab('EARNINGS')}
          id="tab-btn-earnings"
        >
          <DollarSign size={20} />
          <span>Earnings</span>
        </button>

        <button
          className={`nav-item ${activeTab === 'HISTORY' ? 'active' : ''}`}
          onClick={() => setActiveTab('HISTORY')}
          id="tab-btn-history"
        >
          <Clock size={20} />
          <span>History</span>
        </button>

        <button
          className={`nav-item ${activeTab === 'PROFILE' ? 'active' : ''}`}
          onClick={() => setActiveTab('PROFILE')}
          id="tab-btn-profile"
        >
          <User size={20} />
          <span>Settings</span>
        </button>
      </nav>

      {/* Incoming Delivery Offer Modal */}
      {incomingOffer && (
        <IncomingOfferModal
          delivery={incomingOffer}
          onAccept={handleAcceptOffer}
          onDecline={handleDeclineOffer}
        />
      )}

      {/* OTP Proof of Delivery Verification Modal */}
      {showOtpModal && activeDelivery && (
        <OtpVerificationModal
          onClose={() => setShowOtpModal(false)}
          onVerify={handleVerifyOtpAndComplete}
          expectedOtp={activeDelivery.deliveryOtp}
          loading={loading}
        />
      )}

      {/* Failure Reporting Modal */}
      {showFailureModal && (
        <FailureReportModal
          onClose={() => setShowFailureModal(false)}
          onSubmit={handleReportFailure}
          loading={loading}
        />
      )}

      {/* Driver Login / Register Modal */}
      {showLoginModal && (
        <LoginModal
          onClose={() => setShowLoginModal(false)}
          onSuccess={(d) => {
            setDriver(d);
            setIsLiveBackend(true);
          }}
        />
      )}
    </div>
  );
};

export default App;
