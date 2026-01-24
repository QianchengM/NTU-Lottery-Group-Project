import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Index from './Page/Index.jsx';
import Mall from './Page/Mall.jsx'
import './App.css'

function App() {
  return (
      <Router>
          <div className="min-h-screen bg-gray-50">
              {/* 全局导航栏，替代原有 HTML 中的 <nav> 部分 */}


              <Routes>
                  <Route path="/" element={<Index />} />
                  <Route path="/mall" element={<Mall />} />
              </Routes>
          </div>
      </Router>
  )
}

export default App
