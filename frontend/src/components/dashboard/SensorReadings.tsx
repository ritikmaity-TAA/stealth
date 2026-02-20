import React from 'react';
import { SensorData } from './dashboardData';

interface SensorReadingsProps {
  isDarkMode: boolean;
  data?: (SensorData & { co?: number }) | null;
}

const SensorReadings: React.FC<SensorReadingsProps> = ({ data }) => {
  return (
    <section className="sensor-section">
      <p className="sensor-header">── REAL TIME DATA ──</p>
      <ul className="sensor-list">
        <li>
          <div className="sensor-icon pm25">🟤</div>
          <span className="sensor-label">PM2.5</span>
          <span className="sensor-value">
            {data?.pm25 != null ? `${data.pm25} µg/m³` : '—'}
          </span>
        </li>
        <li>
          <div className="sensor-icon pm10">⚪️</div>
          <span className="sensor-label">PM10</span>
          <span className="sensor-value">
            {data?.pm10 != null ? `${data.pm10} µg/m³` : '—'}
          </span>
        </li>
        {/* Added Carbon Monoxide reading */}
        <li>
          <div className="sensor-icon co">💨</div>
          <span className="sensor-label">CO</span>
          <span className="sensor-value">
            {data?.co != null ? `${data.co} µg/m³` : '—'}
          </span>
        </li>
      </ul>
    </section>
  );
};

export default SensorReadings;