# 🛵 SwiftPulse Driver Companion App

A modern, responsive, high-performance web & mobile companion app built for delivery drivers, couriers, and dispatch operations. Built with **React 19**, **TypeScript**, **Vite**, **Leaflet Maps**, and **STOMP WebSocket**.

---

## ✨ Features

- **⚡ Real-Time Shift Management**:
  - One-tap **Online / Offline** shift toggle with glowing pulse ring indicator.
  - Active sonar radar searching animation when waiting for new order assignments.

- **🗺️ Interactive Map & Telemetry (Leaflet)**:
  - Dark sleek cartography tailored for night and day driving.
  - Directional vehicle pin with heading pointer.
  - Store pickup pin, customer dropoff pin, and dynamic route polylines.
  - **Live GPS Simulation Engine**: "Simulate Drive" animates the driver along the route while broadcasting telemetry to the backend (`POST /api/v1/drivers/me/location`).

- **📦 Complete 5-Stage Delivery State Machine**:
  1. **Incoming Delivery Offer**: Audio alert chime, guaranteed payout banner (`$14.80`), 45-second countdown timer matching Spring backend config, trip distance, and Accept / Decline actions.
  2. **En Route to Pickup**: Navigation guidance and store contact.
  3. **Arrived at Store**: Collect order and review item checklist.
  4. **Out for Delivery**: En route to customer with direct call shortcut.
  5. **Proof of Delivery (OTP)**: 4-digit customer verification code input with auto-advance and celebration confetti burst!
  6. **Incident / Failure Reporting**: Incident reason logging (customer unreachable, refused, damaged package).

- **💰 Earnings & Settlement Dashboard**:
  - Today's earnings hero card, lifetime balance, and completed delivery counters.
  - Recent trip settlement logs with status badges (`PROCESSED`, `PENDING`).

- **📜 Order History & Audit Trail**:
  - Filterable by `All`, `Delivered`, and `Failed`.
  - Recipient names, timestamps, and order identifiers.

- **🔌 Dual-Mode Architecture**:
  - **Live Backend Mode**: Directly communicates with Spring Boot endpoints at `http://localhost:8080/api/v1` with JWT authentication and WebSocket STOMP.
  - **Sandbox / Demo Mode**: Instant preview and testing with built-in test order generator.

---

## 🚀 Getting Started

### Prerequisites
- Node.js v18+ (tested on v24.18)
- npm v10+

### Run Dev Server
```bash
cd driver-app
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173) in your browser.
