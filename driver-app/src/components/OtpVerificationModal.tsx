import React, { useState, useRef } from 'react';
import { ShieldCheck, X, Camera, Check } from 'lucide-react';
import { DeliveryCompletionPayload } from '../types/driver';

interface OtpVerificationModalProps {
  onClose: () => void;
  onVerify: (payload: DeliveryCompletionPayload) => void;
  expectedOtp?: string;
  loading: boolean;
}

export const OtpVerificationModal: React.FC<OtpVerificationModalProps> = ({
  onClose,
  onVerify,
  expectedOtp,
  loading,
}) => {
  const [digits, setDigits] = useState(['', '', '', '']);
  const [recipientName, setRecipientName] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const inputRefs = [
    useRef<HTMLInputElement>(null),
    useRef<HTMLInputElement>(null),
    useRef<HTMLInputElement>(null),
    useRef<HTMLInputElement>(null),
  ];

  const handleDigitChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;

    const newDigits = [...digits];
    newDigits[index] = value.slice(-1);
    setDigits(newDigits);
    setError(null);

    // Auto-advance
    if (value && index < 3) {
      inputRefs[index + 1].current?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs[index - 1].current?.focus();
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const enteredOtp = digits.join('');

    if (enteredOtp.length < 4) {
      setError('Please enter the complete 4-digit customer OTP');
      return;
    }

    if (expectedOtp && enteredOtp !== expectedOtp) {
      setError(`Incorrect OTP! (Hint for testing: ${expectedOtp})`);
      return;
    }

    onVerify({
      otp: enteredOtp,
      recipientName: recipientName || undefined,
      notes: notes || undefined,
    });
  };

  return (
    <div className="modal-backdrop">
      <div className="offer-modal" style={{ borderColor: 'var(--primary)' }} id="otp-modal-dialog">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '50%',
                background: 'rgba(99, 102, 241, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary)',
              }}
            >
              <ShieldCheck size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '16px', fontFamily: 'var(--font-heading)', fontWeight: 700 }}>
                Proof of Delivery
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                Ask recipient for 4-digit verification code
              </p>
            </div>
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

        {/* Expected OTP Hint for fast developer testing */}
        {expectedOtp && (
          <div
            style={{
              fontSize: '11.5px',
              color: 'var(--cyan)',
              background: 'rgba(6, 182, 212, 0.1)',
              border: '1px solid rgba(6, 182, 212, 0.25)',
              padding: '6px 10px',
              borderRadius: 'var(--radius-sm)',
              textAlign: 'center',
            }}
          >
            Demo / Backend OTP: <strong>{expectedOtp}</strong>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* OTP Input Grid */}
          <div className="otp-input-grid">
            {digits.map((digit, i) => (
              <input
                key={i}
                ref={inputRefs[i]}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleDigitChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                className="otp-digit-input"
                id={`otp-input-${i}`}
                autoFocus={i === 0}
              />
            ))}
          </div>

          {error && (
            <div
              style={{
                color: 'var(--rose)',
                fontSize: '12px',
                textAlign: 'center',
                fontWeight: 500,
              }}
            >
              {error}
            </div>
          )}

          {/* Optional recipient info */}
          <div className="form-group">
            <label className="form-label">Recipient Name (Optional)</label>
            <input
              type="text"
              placeholder="e.g. Sarah J. / Front Desk"
              className="form-input"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              id="recipient-name-input"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Delivery Notes / Handover Spot</label>
            <input
              type="text"
              placeholder="e.g. Handed directly to customer at door"
              className="form-input"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              id="delivery-notes-input"
            />
          </div>

          <button
            type="submit"
            className="action-cta-btn emerald"
            id="submit-otp-completion-btn"
            disabled={loading}
          >
            {loading ? 'Verifying...' : 'Confirm Delivery'}
          </button>
        </form>
      </div>
    </div>
  );
};
