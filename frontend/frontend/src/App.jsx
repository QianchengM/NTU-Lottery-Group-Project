import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Auth from './Page/Auth.jsx';
import Index from './Page/Index.jsx';
import Mall from './Page/Mall.jsx'
import History from './Page/History.jsx'
import './App.css'

function App() {
  return (
      <Router>
          <div className="min-h-screen bg-gray-50">
              <Routes>
                  <Route path="/" element={<Auth />} />
                  <Route path="/home" element={<Index />} />
                  <Route path="/mall" element={<Mall />} />
                  <Route path="/history" element={<History />} />
              </Routes>
          </div>
      </Router>
  )
}

export default App
