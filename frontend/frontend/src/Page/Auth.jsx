import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import './Auth.css';

const initialLogin = { userId: '', password: '' };
const initialRegister = { userId: '', password: '', inviteCode: '' };

const Auth = () => {
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [loginForm, setLoginForm] = useState(initialLogin);
  const [registerForm, setRegisterForm] = useState(initialRegister);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);

  const title = useMemo(
    () => (mode === 'login' ? 'Welcome Back' : 'Create Your Lucky Account'),
    [mode]
  );

  const handleLogin = async (event) => {
    event.preventDefault();
    setMessage(null);

    if (!loginForm.userId || !loginForm.password) {
      setMessage('Please enter user ID and password');
      return;
    }

    const loginUserId = Number(loginForm.userId);
    if (!Number.isFinite(loginUserId) || loginUserId <= 0) {
      setMessage('User ID must be a positive number');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/api/user/login', {
        userId: loginUserId,
        password: loginForm.password,
      });
      const nextUserId = res?.userId ?? loginForm.userId;
      localStorage.setItem('userId', String(nextUserId));
      setMessage(res?.message || 'Login successful. Redirecting...');
      navigate('/home');
    } catch (e) {
      setMessage(e?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setMessage(null);

    if (!registerForm.userId || !registerForm.password) {
      setMessage('User ID and password are required');
      return;
    }

    const registerUserId = Number(registerForm.userId);
    if (!Number.isFinite(registerUserId) || registerUserId <= 0) {
      setMessage('User ID must be a positive number');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/api/user/register', {
        userId: registerUserId,
        password: registerForm.password,
        inviteCode: registerForm.inviteCode || null,
      });
      const nextUserId = res?.userId ?? registerForm.userId;
      localStorage.setItem('userId', String(nextUserId));
      setMessage(res?.message || 'Registration successful.');
      navigate('/home');
    } catch (e) {
      setMessage(e?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-hero">
        <div className="auth-brand">
          <span className="auth-badge">NTU Lottery</span>
          <h1>{title}</h1>
          <p>
            Register with an invite code and both you and your inviter get 100 points to spend on draws and rewards.
          </p>
        </div>
        <div className="auth-highlight">
          <div className="highlight-card">
            <h2>New User Bonus</h2>
            <p>Start strong with extra points and better odds on your first draw.</p>
          </div>
          <div className="highlight-card">
            <h2>Invite Rewards</h2>
            <p>Use a friend's code and both of you get +100 points instantly.</p>
          </div>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-tabs">
          <button
            type="button"
            className={mode === 'login' ? 'active' : ''}
            onClick={() => {
              setMode('login');
              setMessage(null);
            }}
          >
            Login
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'active' : ''}
            onClick={() => {
              setMode('register');
              setMessage(null);
            }}
          >
            Register
          </button>
        </div>

        {mode === 'login' ? (
          <form className="auth-form" onSubmit={handleLogin}>
            <label>
              User ID
              <input
                type="text"
                inputMode="numeric"
                placeholder="Enter your user ID"
                value={loginForm.userId}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, userId: event.target.value }))
                }
              />
            </label>
            <label>
              Password
              <input
                type="password"
                placeholder="Enter your password"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, password: event.target.value }))
                }
              />
            </label>
            <button className="primary" type="submit" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleRegister}>
            <label>
              User ID
              <input
                type="text"
                inputMode="numeric"
                placeholder="Choose a user ID"
                value={registerForm.userId}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, userId: event.target.value }))
                }
              />
            </label>
            <label>
              Password
              <input
                type="password"
                placeholder="Create a password"
                value={registerForm.password}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, password: event.target.value }))
                }
              />
            </label>
            <label>
              Invite Code (Optional)
              <input
                type="text"
                placeholder="Enter your friend's code"
                value={registerForm.inviteCode}
                onChange={(event) =>
                  setRegisterForm((prev) => ({ ...prev, inviteCode: event.target.value }))
                }
              />
            </label>
            <div className="auth-tip">With a code, both users receive +100 points.</div>
            <button className="primary" type="submit" disabled={loading}>
              {loading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
        )}

        {message && <div className="auth-message">{message}</div>}
      </div>
    </div>
  );
};

export default Auth;
