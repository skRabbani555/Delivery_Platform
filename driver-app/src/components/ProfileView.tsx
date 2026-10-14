import React from 'react';
import {
  User,
  Star,
  Bike,
  Shield,
  Smartphone,
  Server,
  Zap,
  LogOut,
  RefreshCw,
  Sliders,
  CheckCircle2,
} from 'lucide-react';
import { Driver, DriverStatus } from '../types/driver';

interface ProfileViewProps {
  driver: Driver;
  onUpdateStatus: (status: DriverStatus) => void;
  isLiveBackend: boolean;
  onToggleBackendMode: () => void;
  onOpenLoginModal: () => void;
  onSimulateOrder: () => void;
  onLogout: () => void;
}

export const ProfileView: React.FC<ProfileViewProps> = ({
  driver,
  onUpdateStatus,
  isLiveBackend,
  onToggleBackendMode,
  onOpenLoginModal,
  onSimulateOrder,
  onLogout,
}) => {
  return (
    <div className="scroll-viewport" id="profile-view-container">
      <h2 className="section-title">
        <User size={20} color="var(--primary)" />
        Driver Profile & Settings
      </h2>

      {/* Driver Identity Card */}
      <div
        style={{
          background: 'var(--bg-surface-elevated)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          gap: '12px',
        }}
      >
        <div className="avatar-badge" style={{ width: '64px', height: '64px', fontSize: '22px' }}>
          {driver.name.charAt(0)}
        </div>

        <div>
          <h3 style={{ fontSize: '18px', fontWeight: 700, fontFamily: 'var(--font-heading)' }}>
            {driver.name}
          </h3>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{driver.email}</p>
          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{driver.phone}</p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: '12px',
              fontWeight: 700,
              color: 'var(--amber)',
              background: 'rgba(245, 158, 11, 0.12)',
              padding: '4px 10px',
              borderRadius: 'var(--radius-full)',
            }}
          >
            <Star size={13} fill="currentColor" />
            {driver.rating || 4.96} Rating
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: '12px',
              fontWeight: 600,
              color: 'var(--cyan)',
              background: 'rgba(6, 182, 212, 0.12)',
              padding: '4px 10px',
              borderRadius: 'var(--radius-full)',
            }}
          >
            <Bike size={13} />
            {driver.vehicleType || 'Electric E-Bike'}
          </div>
        </div>
      </div>

      {/* Quick Shift Status Selection */}
      <div className="stat-card">
        <span className="stat-card-label">Current Shift State</span>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px', marginTop: '8px' }}>
          {(['AVAILABLE', 'BUSY', 'OFFLINE'] as DriverStatus[]).map((st) => (
            <button
              key={st}
              onClick={() => onUpdateStatus(st)}
              className="quick-action-btn"
              style={{
                justifyContent: 'center',
                padding: '8px',
                fontSize: '12px',
                background: driver.status === st ? 'var(--primary)' : 'rgba(255, 255, 255, 0.04)',
                color: driver.status === st ? 'white' : 'var(--text-secondary)',
                borderColor: driver.status === st ? 'var(--primary)' : 'var(--border-subtle)',
              }}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Backend & Simulation Engine Controls */}
      <div className="stat-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Server size={18} color="var(--primary)" />
            <span style={{ fontSize: '14px', fontWeight: 700 }}>Connection Mode</span>
          </div>

          <span
            style={{
              fontSize: '11px',
              fontWeight: 700,
              padding: '3px 8px',
              borderRadius: 'var(--radius-full)',
              background: isLiveBackend ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.15)',
              color: isLiveBackend ? 'var(--emerald)' : 'var(--primary)',
            }}
          >
            {isLiveBackend ? 'Live Spring Backend (:8080)' : 'Sandbox / Demo Mode'}
          </span>
        </div>

        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '6px' }}>
          {isLiveBackend
            ? 'Connected to Spring Boot backend for JWT authentication and dispatch events.'
            : 'Standalone demo mode enabled for instant UI preview and full delivery workflow tests.'}
        </p>

        <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
          <button
            onClick={onToggleBackendMode}
            className="quick-action-btn"
            id="toggle-backend-mode-btn"
            style={{ flex: 1, justifyContent: 'center' }}
          >
            <RefreshCw size={14} />
            Switch to {isLiveBackend ? 'Sandbox' : 'Live Backend'}
          </button>

          {isLiveBackend && (
            <button
              onClick={onOpenLoginModal}
              className="quick-action-btn"
              style={{ flex: 1, justifyContent: 'center', color: 'var(--emerald)', borderColor: 'var(--emerald-glow)' }}
            >
              <Shield size={14} />
              Driver Login
            </button>
          )}
        </div>
      </div>

      {/* Sandbox Test Tools */}
      <div className="stat-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Zap size={18} color="var(--amber)" />
          <span style={{ fontSize: '14px', fontWeight: 700 }}>Developer Simulation Tool</span>
        </div>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>
          Dispatch a new high-priority delivery request to test sound alerts, countdown timer, and route updates.
        </p>
        <button
          onClick={onSimulateOrder}
          className="action-cta-btn amber"
          id="trigger-simulated-order-btn"
          style={{ marginTop: '10px', padding: '10px' }}
        >
          <Zap size={16} />
          Trigger Incoming Order Offer
        </button>
      </div>

      {/* Logout */}
      <button
        onClick={onLogout}
        className="action-cta-btn"
        style={{
          background: 'rgba(244, 63, 94, 0.12)',
          border: '1px solid var(--rose-glow)',
          color: 'var(--rose)',
          padding: '12px',
          marginTop: '10px',
        }}
      >
        <LogOut size={16} />
        Log Out Session
      </button>
    </div>
  );
};
