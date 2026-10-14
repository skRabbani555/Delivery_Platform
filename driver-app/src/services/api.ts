import {
  Driver,
  DriverStatus,
  Delivery,
  DriverEarningsSummary,
  DriverPayout,
  DeliveryCompletionPayload,
  DeliveryFailurePayload,
  LocationCoordinates,
} from '../types/driver';

const TOKEN_KEY = 'swiftpulse_driver_token';
const DRIVER_KEY = 'swiftpulse_driver_info';

export class DriverApiService {
  private baseUrl: string = '/api/v1';

  public getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  public setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  public clearAuth(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(DRIVER_KEY);
  }

  public getCachedDriver(): Driver | null {
    const raw = localStorage.getItem(DRIVER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  public setCachedDriver(driver: Driver): void {
    localStorage.setItem(DRIVER_KEY, JSON.stringify(driver));
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...((options.headers as Record<string, string>) || {}),
    };

    const res = await fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers,
    });

    if (!res.ok) {
      let errorMessage = `HTTP error ${res.status}`;
      try {
        const errorJson = await res.json();
        errorMessage = errorJson.message || errorJson.error || JSON.stringify(errorJson);
      } catch {
        // Fallback to text
        const text = await res.text();
        if (text) errorMessage = text;
      }
      throw new Error(errorMessage);
    }

    if (res.status === 204) {
      return {} as T;
    }

    return res.json();
  }

  // ── Auth & Registration ───────────────────────────────────────

  async login(email: string, password: string): Promise<{ accessToken: string; tokenType: string }> {
    const res = await this.request<{ accessToken: string; tokenType: string }>(
      '/driver/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }
    );
    this.setToken(res.accessToken);
    return res;
  }

  async register(data: {
    name: string;
    email: string;
    phone: string;
    password: string;
    vehicleType?: string;
    vehiclePlate?: string;
  }): Promise<Driver> {
    return this.request<Driver>('/drivers', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // ── Profile & Status ──────────────────────────────────────────

  async getProfile(): Promise<Driver> {
    const driver = await this.request<Driver>('/drivers/me/status');
    this.setCachedDriver(driver);
    return driver;
  }

  async updateStatus(status: DriverStatus): Promise<Driver> {
    const driver = await this.request<Driver>(`/drivers/me/status?status=${status}`, {
      method: 'PATCH',
    });
    this.setCachedDriver(driver);
    return driver;
  }

  // ── Delivery Lifecycle ────────────────────────────────────────

  async getActiveDelivery(): Promise<Delivery | null> {
    try {
      return await this.request<Delivery>('/drivers/me/delivery');
    } catch {
      return null;
    }
  }

  async acceptDelivery(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/accept', {
      method: 'POST',
    });
  }

  async rejectDelivery(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/reject', {
      method: 'POST',
    });
  }

  async arrivedAtPickup(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/arrived-pickup', {
      method: 'POST',
    });
  }

  async pickupOrder(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/pickup', {
      method: 'POST',
    });
  }

  async outForDelivery(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/out-for-delivery', {
      method: 'POST',
    });
  }

  async arrivedAtDrop(): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/arrived-drop', {
      method: 'POST',
    });
  }

  async completeDelivery(payload: DeliveryCompletionPayload): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/complete', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  async failDelivery(payload: DeliveryFailurePayload): Promise<Delivery> {
    return this.request<Delivery>('/drivers/me/delivery/fail', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  // ── Earnings, Payouts & History ───────────────────────────────

  async getEarningsSummary(): Promise<DriverEarningsSummary> {
    return this.request<DriverEarningsSummary>('/drivers/me/earnings');
  }

  async getPayouts(): Promise<DriverPayout[]> {
    return this.request<DriverPayout[]>('/drivers/me/payouts');
  }

  async getDeliveryHistory(status?: string): Promise<Delivery[]> {
    const query = status ? `?status=${status}` : '';
    return this.request<Delivery[]>(`/drivers/me/deliveries${query}`);
  }

  // ── Location Telemetry ────────────────────────────────────────

  async updateLocation(coords: LocationCoordinates): Promise<Driver> {
    return this.request<Driver>('/drivers/me/location', {
      method: 'POST',
      body: JSON.stringify({
        latitude: coords.latitude,
        longitude: coords.longitude,
        heading: coords.heading ?? 0,
        speed: coords.speed ?? 0,
        accuracy: coords.accuracy ?? 5,
      }),
    });
  }
}

export const api = new DriverApiService();
