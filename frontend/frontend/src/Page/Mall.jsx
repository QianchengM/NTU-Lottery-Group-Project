import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Gift, ShoppingBag, ArrowLeft } from 'lucide-react';
import { api } from '../lib/api.js';

const USER_ID = 1; // demo user

const Mall = () => {
  const [products, setProducts] = useState([]);
  const [points, setPoints] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);

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
    // Heuristic: show items that have a point_cost (or price) defined
    return products
      .filter((p) => (p?.point_cost ?? 0) > 0)
      .sort((a, b) => (a.point_cost ?? 0) - (b.point_cost ?? 0));
  }, [products]);

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

  const ProductCard = ({ product }) => (
    <div className="bg-white rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow border border-slate-100 flex flex-col">
      <div className="flex items-start justify-between gap-3 mb-4">
        <div>
          <h4 className="text-lg font-bold text-slate-800">{product.name}</h4>
          <p className="text-sm text-slate-500">库存: {product.stock}</p>
        </div>
        <div className="w-12 h-12 bg-indigo-50 rounded-xl flex items-center justify-center text-indigo-600">
          <ShoppingBag size={22} />
        </div>
      </div>

      <div className="mt-auto">
        <div className="text-rose-500 font-extrabold text-xl mb-4">{product.point_cost} 积分</div>
        <button
          disabled={loading || product.stock <= 0}
          onClick={() => exchange(product.id)}
          className={`w-full py-2 rounded-xl transition-colors font-semibold ${
            loading || product.stock <= 0
              ? 'bg-slate-200 text-slate-500'
              : 'bg-slate-900 text-white hover:bg-slate-800'
          }`}
        >
          {product.stock <= 0 ? '已售罄' : '立即兑换'}
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <nav className="max-w-6xl mx-auto flex justify-between items-center bg-white p-4 rounded-2xl shadow-sm mb-8">
        <h1 className="text-2xl font-bold text-indigo-600 flex items-center gap-2">
          <Gift size={28} /> NTU Lottery Mall
        </h1>
        <div className="flex items-center gap-4 text-slate-600">
          <div className="bg-amber-100 text-amber-700 px-4 py-1.5 rounded-full font-semibold">
            💰 {points ?? '...'} Points
          </div>
          <Link to="/" className="hover:text-indigo-600 flex items-center gap-1">
            <ArrowLeft size={18} /> Back
          </Link>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto">
        {message && (
          <div className="mb-6 p-4 rounded-xl bg-white border border-slate-100 text-slate-700 font-semibold">
            {message}
          </div>
        )}

        {mallProducts.length === 0 ? (
          <div className="bg-white p-8 rounded-2xl border border-slate-100 text-slate-600">
            暂无可兑换商品（需要 prize.point_cost &gt; 0）。
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {mallProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Mall;
