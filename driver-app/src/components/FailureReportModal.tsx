import React, { useState } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { DeliveryFailurePayload } from '../types/driver';

interface FailureReportModalProps {
  onClose: () => void;
  onSubmit: (payload: DeliveryFailurePayload) => void;
  loading: boolean;
}

const FAILURE_REASONS = [
  'Customer unreachable / Phone switched off',
  'Incorrect / Incomplete delivery address',
  'Customer refused delivery',
  'Restricted access / Gate locked',
  'Damaged package in transit',
];

export const FailureReportModal: React.FC<FailureReportModalProps> = ({
  onClose,
  onSubmit,
  loading,
}) => {
  const [selectedReason, setSelectedReason] = useState(FAILURE_REASONS[0]);
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      reason: selectedReason,
      notes: notes || undefined,
    });
  };

  return (
    <div className="modal-backdrop">
      <div className="offer-modal" style={{ borderColor: 'var(--rose)' }} id="failure-modal-dialog">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '50%',
                background: 'rgba(244, 63, 94, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--rose)',
              }}
            >
              <AlertTriangle size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '16px', fontFamily: 'var(--font-heading)', fontWeight: 700 }}>
                Report Delivery Issue
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                Order will be marked as failed and store will be notified
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

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div className="form-group">
            <label className="form-label">Select Reason</label>
            <select
              className="form-input"
              value={selectedReason}
              onChange={(e) => setSelectedReason(e.target.value)}
              id="failure-reason-select"
              style={{ background: 'var(--bg-surface)' }}
            >
              {FAILURE_REASONS.map((r, i) => (
                <option key={i} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Additional Incident Details</label>
            <textarea
              className="form-input"
              rows={3}
              placeholder="e.g. Called customer 3 times, waited 10 mins outside building..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              id="failure-notes-input"
            />
          </div>

          <button
            type="submit"
            className="action-cta-btn"
            id="confirm-failure-btn"
            style={{
              background: 'linear-gradient(135deg, #ef4444, #dc2626)',
              boxShadow: '0 8px 24px -4px rgba(239, 68, 68, 0.5)',
            }}
            disabled={loading}
          >
            {loading ? 'Submitting...' : 'Confirm Delivery Failure'}
          </button>
        </form>
      </div>
    </div>
  );
};
