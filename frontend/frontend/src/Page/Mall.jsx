import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Gift, ShoppingBag, ArrowLeft, Sparkles, LogOut } from 'lucide-react';
import { api } from '../lib/api.js';
import './Mall.css';

const USER_ID = Number(localStorage.getItem('userId')) || 1;

const Mall = () => {
  const [products, setProducts] = useState([]);
  const [points, setPoints] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const fetchAll = async () => {
    const [list, p] = await Promise.all([
      api.get('/api/prizes'),
      api.get(`/api/user/points?userId=${USER_ID}`),
    ]);
    setProducts(Array.isArray(list) ? list : []);
    setPoints(p);
  };

  useEffect(() => {
    fetchAll().catch((e) => setMessage(e?.message || 'Failed to load'));
  }, []);

  const mallProducts = useMemo(() => {
    return products
      .filter((p) => (p?.point_cost ?? 0) > 0)
      .sort((a, b) => (a.point_cost ?? 0) - (b.point_cost ?? 0));
  }, [products]);

  const handleLogout = () => {
    localStorage.removeItem('userId');
    navigate('/');
  };

  const exchange = async (prizeId) => {
    setLoading(true);
    setMessage(null);
    try {
      const msg = await api.get(`/api/mall/exchange?userId=${USER_ID}&prizeId=${prizeId}`);
      setMessage(msg);
      await fetchAll();
    } catch (e) {
      setMessage(e?.message || 'Exchange failed');
      await fetchAll().catch(() => {});
    } finally {
      setLoading(false);
    }
  };

  const ProductCard = ({ product, index }) => (
    <div className="mall-card" style={{ animationDelay: `${index * 80}ms` }}>
      <div className="mall-card-header">
        <div>
          <h4>{product.name}</h4>
          <p>Stock: {product.stock}</p>
        </div>
        <div className="mall-card-icon">
          <ShoppingBag size={22} />
        </div>
      </div>

      <div className="mall-card-footer">
        <div className="mall-card-price">{product.point_cost} Points</div>
        <button
          disabled={loading || product.stock <= 0}
          onClick={() => exchange(product.id)}
          className={product.stock <= 0 ? 'disabled' : ''}
        >
          {product.stock <= 0 ? 'Sold Out' : 'Redeem'}
        </button>
      </div>
    </div>
  );

  return (
    <div className="mall-page">
      <nav className="mall-nav">
        <div className="mall-title">
          <Gift size={28} />
          <div>
            <span>NTU Lottery Mall</span>
            <small>Redeem rewards with your points</small>
          </div>
        </div>
        <div className="mall-actions">
          <div className="mall-points">
            <Sparkles size={16} />
            <span>{points ?? '...'} Points</span>
          </div>
          <Link to="/home" className="mall-back">
            <ArrowLeft size={18} />
            Back to Draws
          </Link>
          <button className="mall-logout" onClick={handleLogout}>
            <LogOut size={18} />
            Log Out
          </button>
        </div>
      </nav>

      <section className="mall-hero">
        <h2>Rewards Catalog</h2>
        <p>Pick your favorites and exchange points for prizes.</p>
      </section>

      <div className="mall-content">
        {message && <div className="mall-message">{message}</div>}

        {mallProducts.length === 0 ? (
          <div className="mall-empty">No items available (requires prize.point_cost &gt; 0).</div>
        ) : (
          <div className="mall-grid">
            {mallProducts.map((p, index) => (
              <ProductCard key={p.id} product={p} index={index} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Mall;
