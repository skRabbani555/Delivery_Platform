import React, { useState } from 'react';
import { X, Lock, Mail, User, Phone, Bike, AlertCircle } from 'lucide-react';
import { api } from '../services/api';
import { Driver } from '../types/driver';

interface LoginModalProps {
  onClose: () => void;
  onSuccess: (driver: Driver) => void;
}

export const LoginModal: React.FC<LoginModalProps> = ({ onClose, onSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [vehicleType, setVehicleType] = useState('Motorcycle / Scooter');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        const newDriver = await api.register({
          name,
          email,
          phone,
          password,
          vehicleType,
        });
        // Auto-login after registration
        await api.login(email, password);
        onSuccess(newDriver);
      } else {
        await api.login(email, password);
        const driver = await api.getProfile();
        onSuccess(driver);
      }
      onClose();
    } catch (err: any) {
      setError(err.message || 'Authentication failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="offer-modal" style={{ borderColor: 'var(--primary)' }} id="auth-modal-dialog">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '18px', fontFamily: 'var(--font-heading)', fontWeight: 700 }}>
              {isRegister ? 'Driver Registration' : 'Driver Portal Login'}
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              Connect directly to Spring Boot Backend
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
            }}
          >
            <X size={20} />
          </button>
        </div>

        {error && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 14px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(244, 63, 94, 0.12)',
              border: '1px solid var(--rose-glow)',
              color: 'var(--rose)',
              fontSize: '12px',
            }}
          >
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {isRegister && (
            <>
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. David Miller"
                  className="form-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Phone Number</label>
                <input
                  type="tel"
                  required
                  placeholder="e.g. +1 555 019 2831"
                  className="form-input"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Vehicle Type</label>
                <select
                  className="form-input"
                  value={vehicleType}
                  onChange={(e) => setVehicleType(e.target.value)}
                  style={{ background: 'var(--bg-surface)' }}
                >
                  <option value="Electric E-Bike">Electric E-Bike</option>
                  <option value="Motorcycle / Scooter">Motorcycle / Scooter</option>
                  <option value="Standard Bicycle">Standard Bicycle</option>
                  <option value="Compact Car">Compact Car</option>
                </select>
              </div>
            </>
          )}

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              required
              placeholder="driver@example.com"
              className="form-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              id="auth-email-input"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              required
              placeholder="••••••••"
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              id="auth-password-input"
            />
          </div>

          <button
            type="submit"
            className="action-cta-btn indigo"
            disabled={loading}
            id="auth-submit-btn"
            style={{ marginTop: '6px' }}
          >
            {loading ? 'Processing...' : isRegister ? 'Complete Registration' : 'Log In & Authenticate'}
          </button>
        </form>

        <div style={{ textAlign: 'center', fontSize: '12px', color: 'var(--text-secondary)' }}>
          {isRegister ? 'Already registered?' : 'New driver onboard?'}{' '}
          <button
            type="button"
            onClick={() => {
              setIsRegister(!isRegister);
              setError(null);
            }}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--primary)',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            {isRegister ? 'Sign In' : 'Create Driver Account'}
          </button>
        </div>
      </div>
    </div>
  );
};
