import React, { useState } from 'react';
import { Package, CheckCircle2, XCircle, Clock, MapPin, ChevronRight } from 'lucide-react';
import { Delivery } from '../types/driver';

interface HistoryViewProps {
  deliveries: Delivery[];
}

export const HistoryView: React.FC<HistoryViewProps> = ({ deliveries }) => {
  const [filter, setFilter] = useState<'ALL' | 'DELIVERED' | 'FAILED'>('ALL');

  const filtered = deliveries.filter((d) => {
    if (filter === 'ALL') return true;
    return d.status === filter;
  });

  return (
    <div className="scroll-viewport" id="history-view-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className="section-title">
          <Clock size={20} color="var(--primary)" />
          Delivery History
        </h2>
        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
          {filtered.length} records
        </span>
      </div>

      {/* Filter Chips */}
      <div style={{ display: 'flex', gap: '8px' }}>
        {(['ALL', 'DELIVERED', 'FAILED'] as const).map((type) => (
          <button
            key={type}
            onClick={() => setFilter(type)}
            className="quick-action-btn"
            style={{
              background: filter === type ? 'var(--primary)' : 'rgba(255, 255, 255, 0.04)',
              color: filter === type ? 'white' : 'var(--text-secondary)',
              borderColor: filter === type ? 'var(--primary)' : 'var(--border-subtle)',
              textTransform: 'capitalize',
            }}
          >
            {type.toLowerCase()}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="data-card-list">
        {filtered.length === 0 ? (
          <div
            style={{
              textAlign: 'center',
              padding: '40px 20px',
              color: 'var(--text-muted)',
              fontSize: '13px',
            }}
          >
            No delivery records found for this filter.
          </div>
        ) : (
          filtered.map((item) => (
            <div
              key={item.id}
              className="data-card-item"
              style={{ flexDirection: 'column', alignItems: 'stretch', gap: '10px' }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Package size={16} color="var(--primary)" />
                  <span style={{ fontSize: '13px', fontWeight: 700 }}>
                    {item.externalOrderId || `Order #${item.id}`}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '14px', fontWeight: 800, color: 'var(--emerald)' }}>
                    +${item.driverPayout.toFixed(2)}
                  </span>
                  <span
                    className={`status-tag ${
                      item.status === 'DELIVERED' ? 'delivered' : 'failed'
                    }`}
                  >
                    {item.status}
                  </span>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-secondary)' }}>
                  <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--amber)' }} />
                  <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {item.pickupAddress}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-secondary)' }}>
                  <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--rose)' }} />
                  <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {item.dropAddress}
                  </span>
                </div>
              </div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  fontSize: '11px',
                  color: 'var(--text-muted)',
                  borderTop: '1px solid var(--border-subtle)',
                  paddingTop: '6px',
                }}
              >
                <span>Recipient: {item.recipientName || item.customerName}</span>
                <span>{item.actualDeliveryTime || 'Delivered'}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
