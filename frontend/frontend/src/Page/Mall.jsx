const Mall = () => {
    // 使用 p.point_cost 或默认值 100
    const ProductCard = ({ product }) => (
        <div className="bg-white rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow border border-slate-100 flex flex-col items-center">
            <div className="w-20 h-20 bg-indigo-50 rounded-full flex items-center justify-center text-indigo-600 mb-4">
                <ShoppingBag size={32} />
            </div>
            <h4 className="text-lg font-bold text-slate-800">{product.name}</h4>
            <p className="text-sm text-slate-500 mb-4">剩余库存: {product.stock}</p>
            <div className="text-rose-500 font-extrabold text-xl mb-6">{product.point_cost || 100} 积分</div>
            <button
                onClick={() => {/* 逻辑同 mall.html */}}
                className="w-full py-2 bg-slate-900 text-white rounded-xl hover:bg-slate-800 transition-colors"
            >
                立即兑换
            </button>
        </div>
    );

    return (
        <div className="max-w-6xl mx-auto p-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* 循环渲染商品卡片 */}
            </div>
        </div>
    );
};
export default Mall;