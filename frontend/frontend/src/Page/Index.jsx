import React, { useState, useEffect } from 'react';
import { Gift, CalendarCheck, ShoppingBag, History, Settings, Trophy } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
const Index = () => {
    const [prizes, setPrizes] = useState([]);
    const [points, setPoints] = useState(1000);
    const [drawing, setDrawing] = useState(false);
    const [result, setResult] = useState(null);
    const userId = 1; // 对应 Alice

    useEffect(() => {
        fetchPrizes();
    }, []);

    const fetchPrizes = () => {
        fetch('/prizes').then(res => res.json()).then(setPrizes);
    };

    const handleDraw = async () => {
        setDrawing(true);
        setResult(null);
        try {
            const res = await fetch(`/draw?userId=${userId}&activityId=1`);
            const text = await res.text();
            setResult(text);
            setPoints(prev => prev - 50); // 对应原逻辑扣分
            fetchPrizes(); // 刷新库存
        } finally {
            setDrawing(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 p-6 font-sans">
            {/* 顶部导航 */}
            <nav className="max-w-6xl mx-auto flex justify-between items-center bg-white p-4 rounded-2xl shadow-sm mb-8">
                <h1 className="text-2xl font-bold text-indigo-600 flex items-center gap-2">
                    <Gift size={28} /> NTU Lottery
                </h1>
                <div className="flex items-center gap-4 text-slate-600">
                    <div className="bg-amber-100 text-amber-700 px-4 py-1.5 rounded-full font-semibold">
                        💰 {points} Points
                    </div>
                    <button className="hover:text-indigo-600 flex items-center gap-1"><CalendarCheck size={18}/> Check-in</button>
                    <button className="hover:text-indigo-600 flex items-center gap-1"><ShoppingBag size={18}/> Mall</button>
                </div>
            </nav>

            <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 左侧抽奖区 */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white p-8 rounded-3xl shadow-xl border border-indigo-50 text-center relative overflow-hidden">
                        <h2 className="text-3xl font-extrabold text-slate-800 mb-2">学期末大抽奖 🎉</h2>
                        <p className="text-slate-500 mb-8">每次消耗 50 积分，100% 中奖概率</p>

                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-8 text-left">
                            {prizes.filter(p => p.activity_id === 1).map(prize => (
                                <div key={prize.id} className="p-4 rounded-xl bg-slate-50 border border-slate-100">
                                    <div className="text-sm text-slate-500">{prize.name}</div>
                                    <div className="font-bold text-slate-800">库存: {prize.stock}</div>
                                </div>
                            ))}
                        </div>

                        <motion.button
                            whileTap={{ scale: 0.95 }}
                            onClick={handleDraw}
                            disabled={drawing}
                            className={`w-full py-4 rounded-2xl text-xl font-bold text-white shadow-lg transition-all ${
                                drawing ? 'bg-slate-400' : 'bg-gradient-to-r from-indigo-600 to-purple-600 hover:opacity-90'
                            }`}
                        >
                            {drawing ? '正在摇奖...' : '🎲 立即参与'}
                        </motion.button>

                        <AnimatePresence>
                            {result && (
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    className={`mt-6 p-4 rounded-xl font-bold ${
                                        result.includes('Congratulations') ? 'bg-green-50 text-green-700' : 'bg-slate-100 text-slate-600'
                                    }`}
                                >
                                    {result}
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>
                </div>

                {/* 右侧边栏: 排行榜 */}
                <div className="space-y-6">
                    <div className="bg-slate-900 rounded-3xl p-6 text-white shadow-xl">
                        <h3 className="text-xl font-bold text-amber-400 mb-4 flex items-center gap-2">
                            <Trophy size={20} /> 幸运星榜单
                        </h3>
                        <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
                            {/* 这里对接 /leaderboard 接口 */}
                            <div className="border-b border-slate-800 pb-2">
                                <div className="text-sm font-medium">Alice 抽中了</div>
                                <div className="text-amber-200">iPad Pro 🎁</div>
                                <div className="text-[10px] text-slate-500">14:20:05</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
export default Index;