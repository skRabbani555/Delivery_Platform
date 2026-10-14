import React from 'react';
import { Star, Wifi, WifiOff } from 'lucide-react';
import { Driver, DriverStatus } from '../types/driver';

interface NavbarProps {
  driver: Driver;
  onToggleShift: () => void;
  onProfileClick: () => void;
  isLiveBackend: boolean;
  todayEarnings: number;
}

export const Navbar: React.FC<NavbarProps> = ({
  driver,
  onToggleShift,
  onProfileClick,
  isLiveBackend,
  todayEarnings,
}) => {
  const isOnline = driver.status !== 'OFFLINE';

  return (
    <header className="top-header" id="app-top-header">
      {/* Driver info chip */}
      <div className="driver-profile-pill" onClick={onProfileClick} id="driver-profile-chip">
        <div className="avatar-badge">
          {driver.name.charAt(0)}
          <span className={`avatar-status-dot ${isOnline ? 'online' : 'offline'}`} />
        </div>
        <div className="driver-meta">
          <span className="driver-name">{driver.name}</span>
          <span className="driver-sub">
            <Star size={11} fill="var(--amber)" color="var(--amber)" />
            {driver.rating || 4.96} • ${todayEarnings.toFixed(0)} today
          </span>
        </div>
      </div>

      {/* Online / Offline Shift Toggle */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button
          className={`shift-toggle-btn ${isOnline ? 'is-online' : 'is-offline'}`}
          id="shift-toggle-btn"
          onClick={onToggleShift}
        >
          {isOnline && <span className="pulse-indicator" />}
          <span>{isOnline ? 'ONLINE' : 'GO ONLINE'}</span>
        </button>
      </div>
    </header>
  );
};
