import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { Navigation, Compass, Layers } from 'lucide-react';
import { Delivery, Driver } from '../types/driver';

interface MapComponentProps {
  driver: Driver;
  activeDelivery: Delivery | null;
  onUpdateCoordinates?: (lat: number, lng: number) => void;
  isDrivingSimulated?: boolean;
}

export const MapComponent: React.FC<MapComponentProps> = ({
  driver,
  activeDelivery,
  onUpdateCoordinates,
  isDrivingSimulated = false,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const driverMarkerRef = useRef<L.Marker | null>(null);
  const storeMarkerRef = useRef<L.Marker | null>(null);
  const dropMarkerRef = useRef<L.Marker | null>(null);
  const routePolylineRef = useRef<L.Polyline | null>(null);
  const animFrameRef = useRef<number | null>(null);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [driver.latitude, driver.longitude],
      zoom: 15,
      zoomControl: false,
      attributionControl: false,
    });

    // Dark sleek map tiles (CartoDB dark matter)
    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      className: 'dark-tiles',
    }).addTo(map);

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Update Driver Marker
  useEffect(() => {
    if (!mapRef.current) return;

    const driverIcon = L.divIcon({
      className: 'driver-marker-wrapper',
      html: `
        <div class="driver-marker-icon" id="driver-pin">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 19 21 12 17 5 21 12 2"></polygon>
          </svg>
        </div>
      `,
      iconSize: [44, 44],
      iconAnchor: [22, 22],
    });

    if (!driverMarkerRef.current) {
      driverMarkerRef.current = L.marker([driver.latitude, driver.longitude], {
        icon: driverIcon,
      }).addTo(mapRef.current);
    } else {
      driverMarkerRef.current.setLatLng([driver.latitude, driver.longitude]);
    }
  }, [driver.latitude, driver.longitude]);

  // Update Delivery Markers & Route
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // Clear old delivery markers & route if none active
    if (!activeDelivery) {
      if (storeMarkerRef.current) {
        map.removeLayer(storeMarkerRef.current);
        storeMarkerRef.current = null;
      }
      if (dropMarkerRef.current) {
        map.removeLayer(dropMarkerRef.current);
        dropMarkerRef.current = null;
      }
      if (routePolylineRef.current) {
        map.removeLayer(routePolylineRef.current);
        routePolylineRef.current = null;
      }
      return;
    }

    const pickupLat = activeDelivery.pickupLat || driver.latitude + 0.006;
    const pickupLng = activeDelivery.pickupLng || driver.longitude + 0.005;
    const dropLat = activeDelivery.dropLat || driver.latitude + 0.012;
    const dropLng = activeDelivery.dropLng || driver.longitude - 0.004;

    // Store Marker (Pickup)
    if (!storeMarkerRef.current) {
      const storeIcon = L.divIcon({
        className: 'store-marker-wrapper',
        html: `
          <div class="store-marker-pin" title="Pickup: ${activeDelivery.pickupAddress}">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
              <polyline points="9 22 9 12 15 12 15 22"></polyline>
            </svg>
          </div>
        `,
        iconSize: [40, 40],
        iconAnchor: [20, 20],
      });

      storeMarkerRef.current = L.marker([pickupLat, pickupLng], { icon: storeIcon }).addTo(map);
    }

    // Drop Marker (Customer)
    if (!dropMarkerRef.current) {
      const dropIcon = L.divIcon({
        className: 'drop-marker-wrapper',
        html: `
          <div class="drop-marker-pin" title="Drop: ${activeDelivery.dropAddress}">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="4"></circle>
            </svg>
          </div>
        `,
        iconSize: [40, 40],
        iconAnchor: [20, 36],
      });

      dropMarkerRef.current = L.marker([dropLat, dropLng], { icon: dropIcon }).addTo(map);
    }

    // Polyline Route
    const routePoints: [number, number][] = [
      [driver.latitude, driver.longitude],
      [pickupLat, pickupLng],
      [dropLat, dropLng],
    ];

    if (!routePolylineRef.current) {
      routePolylineRef.current = L.polyline(routePoints, {
        color: '#6366f1',
        weight: 5,
        opacity: 0.85,
        dashArray: '10, 8',
      }).addTo(map);
    } else {
      routePolylineRef.current.setLatLngs(routePoints);
    }

    // Fit bounds to fit route
    map.fitBounds(L.latLngBounds(routePoints), { padding: [50, 50] });
  }, [activeDelivery, driver.latitude, driver.longitude]);

  // Recenter helper
  const handleRecenter = () => {
    if (mapRef.current) {
      mapRef.current.flyTo([driver.latitude, driver.longitude], 16, {
        duration: 0.8,
      });
    }
  };

  // Route Simulation (Smooth driver animation when driving simulator is active)
  useEffect(() => {
    if (!isDrivingSimulated || !activeDelivery || !onUpdateCoordinates) return;

    const targetLat =
      activeDelivery.status === 'ARRIVED_AT_STORE' || activeDelivery.status === 'PICKED_UP'
        ? activeDelivery.dropLat || driver.latitude + 0.012
        : activeDelivery.pickupLat || driver.latitude + 0.006;
    const targetLng =
      activeDelivery.status === 'ARRIVED_AT_STORE' || activeDelivery.status === 'PICKED_UP'
        ? activeDelivery.dropLng || driver.longitude - 0.004
        : activeDelivery.pickupLng || driver.longitude + 0.005;

    const interval = setInterval(() => {
      const step = 0.0003;
      const deltaLat = targetLat - driver.latitude;
      const deltaLng = targetLng - driver.longitude;
      const distance = Math.hypot(deltaLat, deltaLng);

      if (distance > step) {
        const nextLat = driver.latitude + (deltaLat / distance) * step;
        const nextLng = driver.longitude + (deltaLng / distance) * step;
        onUpdateCoordinates(nextLat, nextLng);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [isDrivingSimulated, activeDelivery, driver.latitude, driver.longitude, onUpdateCoordinates]);

  return (
    <div className="map-view-wrapper">
      <div ref={mapContainerRef} style={{ width: '100%', height: '100%' }} />

      {/* Floating Action Controls */}
      <div className="map-fab-stack">
        <button
          className="fab-btn"
          id="recenter-btn"
          title="Center on my location"
          onClick={handleRecenter}
        >
          <Navigation size={18} />
        </button>
      </div>
    </div>
  );
};
