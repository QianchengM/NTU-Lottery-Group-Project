import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, Gift, ShoppingBag, LogOut } from 'lucide-react';
import { api } from '../lib/api.js';
import './History.css';

const USER_ID = Number(localStorage.getItem('userId')) || 1;

const History = () => {
  const [records, setRecords] = useState([]);
  const [message, setMessage] = useState(null);
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('userId');
    navigate('/');
  };

  const fetchHistory = async () => {
    const data = await api.get(`/api/user/history?userId=${USER_ID}`);
    setRecords(Array.isArray(data) ? data : []);
  };

  useEffect(() => {
    fetchHistory().catch((e) => setMessage(e?.message || 'Failed to load'));
  }, []);

  return (
    <div className="history-page">
      <nav className="history-nav">
        <div className="history-title">
          <Clock size={26} />
          <div>
            <span>My History</span>
            <small>Draws and redemptions</small>
          </div>
        </div>
        <Link to="/home" className="history-back">
          <ArrowLeft size={18} />
          Back to Draws
        </Link>
        <button className="history-logout" onClick={handleLogout}>
          <LogOut size={18} />
          Log Out
        </button>
      </nav>

      <section className="history-hero">
        <h2>Recent Activity</h2>
        <p>All your draw wins and mall exchanges in one place.</p>
      </section>

      <div className="history-content">
        {message && <div className="history-message">{message}</div>}

        {records.length === 0 ? (
          <div className="history-empty">No records yet. Try a draw or redeem points.</div>
        ) : (
          <div className="history-list">
            {records.map((item, index) => {
              const prizeName = String(item.prize_name || '');
              const isMall = prizeName.toLowerCase().includes('mall') || prizeName.toLowerCase().includes('exchange');
              return (
                <div key={`${item.id ?? index}`} className="history-card">
                  <div className="history-card-icon">
                    {isMall ? <ShoppingBag size={18} /> : <Gift size={18} />}
                  </div>
                  <div className="history-card-info">
                    <div className="history-card-title">{item.prize_name}</div>
                    <div className="history-card-meta">
                      Activity: {item.activity_id} · Type: {item.prize_type}
                    </div>
                  </div>
                  <div className="history-card-time">{item.create_time}</div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default History;
