import React from 'react';
import {
  DollarSign,
  TrendingUp,
  CreditCard,
  Calendar,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  ShieldCheck,
} from 'lucide-react';
import { DriverEarningsSummary } from '../types/driver';

interface EarningsViewProps {
  summary: DriverEarningsSummary;
  onRefresh?: () => void;
}

export const EarningsView: React.FC<EarningsViewProps> = ({ summary, onRefresh }) => {
  return (
    <div className="scroll-viewport" id="earnings-view-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className="section-title">
          <DollarSign size={22} color="var(--emerald)" />
          Earnings & Payouts
        </h2>
        {onRefresh && (
          <button
            onClick={onRefresh}
            className="quick-action-btn"
            style={{ fontSize: '11px', padding: '4px 10px' }}
          >
            Refresh
          </button>
        )}
      </div>

      {/* Featured Card */}
      <div className="stat-card featured" id="today-earnings-hero">
        <span className="stat-card-label">Today's Earnings</span>
        <div className="stat-card-amount emerald">
          ${summary.todayEarnings.toFixed(2)}
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: 'var(--text-secondary)',
            marginTop: '4px',
          }}
        >
          <CheckCircle2 size={14} color="var(--emerald)" />
          <span>{summary.todayCompletedDeliveries} trips completed today</span>
        </div>
      </div>

      {/* Grid Stats */}
      <div className="stat-card-row">
        <div className="stat-card">
          <span className="stat-card-label">All-Time Balance</span>
          <div className="stat-card-amount" style={{ fontSize: '22px' }}>
            ${summary.totalEarnings.toFixed(2)}
          </div>
          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
            {summary.totalCompletedDeliveries} lifetime trips
          </span>
        </div>

        <div className="stat-card">
          <span className="stat-card-label">Avg. Payout / Trip</span>
          <div className="stat-card-amount" style={{ fontSize: '22px', color: 'var(--cyan)' }}>
            $
            {summary.totalCompletedDeliveries > 0
              ? (summary.totalEarnings / summary.totalCompletedDeliveries).toFixed(2)
              : '0.00'}
          </div>
          <span style={{ fontSize: '11px', color: 'var(--emerald)' }}>
            +8.4% vs last week
          </span>
        </div>
      </div>

      {/* Instant Payout Action Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(16, 185, 129, 0.1))',
          border: '1px solid var(--border-glow)',
          borderRadius: 'var(--radius-lg)',
          padding: '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: 'rgba(99, 102, 241, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
            }}
          >
            <CreditCard size={20} />
          </div>
          <div>
            <div style={{ fontSize: '13.5px', fontWeight: 700 }}>Direct Bank Deposit</div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-secondary)' }}>
              Automated weekly every Monday at 04:00 AM
            </div>
          </div>
        </div>
      </div>

      {/* Recent Payout Records */}
      <div>
        <h3
          style={{
            fontSize: '14px',
            fontFamily: 'var(--font-heading)',
            color: 'var(--text-secondary)',
            marginBottom: '10px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Recent Trip Settlements
        </h3>

        <div className="data-card-list">
          {summary.recentPayouts.map((p) => (
            <div key={p.id} className="data-card-item">
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '10px',
                    background: 'rgba(16, 185, 129, 0.12)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'var(--emerald)',
                  }}
                >
                  <DollarSign size={18} />
                </div>
                <div>
                  <div style={{ fontSize: '13.5px', fontWeight: 600 }}>
                    Delivery #{p.deliveryId}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    {new Date(p.createdAt).toLocaleDateString([], {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}>
                <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--emerald)' }}>
                  +${p.amount.toFixed(2)}
                </span>
                <span className={`status-tag ${p.status === 'PROCESSED' ? 'delivered' : 'pending'}`}>
                  {p.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
