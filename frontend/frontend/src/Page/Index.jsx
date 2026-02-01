import React, { useState, useEffect } from 'react';
import { Gift, CalendarCheck, ShoppingBag, Trophy, Sparkles, LogOut } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import './Home.css';

const Index = () => {
  const [prizes, setPrizes] = useState([]);
  const [points, setPoints] = useState(null);
  const [drawing, setDrawing] = useState(false);
  const [result, setResult] = useState(null);
  const userId = Number(localStorage.getItem('userId')) || 1;
  const navigate = useNavigate();

  useEffect(() => {
    fetchPrizes();
    fetchPoints();
  }, []);

  const fetchPrizes = async () => {
    const data = await api.get(`/api/prizes?activityId=1`);
    setPrizes(Array.isArray(data) ? data : []);
  };

  const fetchPoints = async () => {
    const p = await api.get(`/api/user/points?userId=${userId}`);
    setPoints(p);
  };

  const handleCheckIn = async () => {
    setResult(null);
    try {
      const msg = await api.get(`/api/user/checkin?userId=${userId}&rewardPoints=50`);
      setResult(msg);
    } catch (e) {
      setResult(e?.message || 'Check-in failed');
    } finally {
      await fetchPoints();
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('userId');
    navigate('/');
  };

  const handleDraw = async () => {
    setDrawing(true);
    setResult(null);
    try {
      const msg = await api.get(`/api/draw?userId=${userId}&activityId=1`);
      setResult(msg);
      await fetchPrizes();
      await fetchPoints();
    } catch (e) {
      setResult(e?.message || 'Draw failed');
    } finally {
      setDrawing(false);
    }
  };

  return (
    <div className="home-page">
      <nav className="home-nav">
        <div className="home-title">
          <Gift size={28} />
          <div>
            <span>NTU Lottery</span>
            <small>Lucky Draw Center</small>
          </div>
        </div>
        <div className="home-actions">
          <div className="home-points">
            <Sparkles size={16} />
            <span>{points ?? '...'} Points</span>
          </div>
          <button className="home-checkin" onClick={handleCheckIn}>
            <CalendarCheck size={18} />
            Check-in
          </button>
          <Link to="/mall" className="home-mall">
            <ShoppingBag size={18} />
            Mall
          </Link>
          <Link to="/history" className="home-mall">
            History
          </Link>
          <button className="home-logout" onClick={handleLogout}>
            <LogOut size={18} />
            Log Out
          </button>
        </div>
      </nav>

      <section className="home-hero">
        <div>
          <h2>Semester Mega Draw</h2>
          <p>Spend 50 points per try with a guaranteed reward. Try your luck today.</p>
        </div>
        <div className="home-hero-tag">Lucky Boost</div>
      </section>

      <div className="home-grid">
        <section className="home-draw">
          <div className="home-prizes">
            {prizes.map((prize) => (
              <div key={prize.id} className="home-prize-card">
                <div>
                  <div className="home-prize-name">{prize.name}</div>
                  <div className="home-prize-stock">Stock: {prize.stock}</div>
                </div>
                <div className="home-prize-dot" />
              </div>
            ))}
          </div>

          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={handleDraw}
            disabled={drawing}
            className="home-draw-button"
          >
            {drawing ? 'Drawing...' : 'Play Now'}
          </motion.button>

          <AnimatePresence>
            {result && (
              <motion.div
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                className={`home-result ${
                  result.includes('Success') || result.includes('Congrats') || result.includes('Congratulations')
                    ? 'home-result-success'
                    : 'home-result-neutral'
                }`}
              >
                {result}
              </motion.div>
            )}
          </AnimatePresence>
        </section>

        <aside className="home-board">
          <div className="home-board-header">
            <Trophy size={20} />
            <div>
              <span>Lucky Star Board</span>
              <small>Live updates</small>
            </div>
          </div>
          <div className="home-board-list">
            <div className="home-board-item">
              <div>
                <div className="home-board-title">Alice won</div>
                <div className="home-board-prize">iPad Pro 🎁</div>
              </div>
              <div className="home-board-time">14:20:05</div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default Index;
