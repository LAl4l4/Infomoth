import {useState} from 'react';
import './Login.css';
import { register } from '../API/auth';
import { useNavigate } from "react-router-dom";

// Front-end only shell. Replace the endpoint/params to match your backend contract.
async function createAccount({email, password, username}) {
  try {
    const params = { 
        username: username, 
        pass: password,
        email: email 
    };

    const res = await register(params.email, params.pass, params.username);

    if (res.data === '用户名已存在') {
      return { success: false, message: res.data };
    }

    if (res.data === '注册成功') {
      return { success: true, message: res.data };
    }

    // Default message when backend returns something else but the call succeeds
    return { success: false, message: res.data || '注册失败' };
  } catch (err) {
    console.error('注册请求失败', err);
    return { success: false, message: '网络错误' };
  }
}

function validateEmail(email) {
    const address = email.split('@');

    if (address.length !== 2) return false;
    
    const domainParts = address[1].split('.');
    if (domainParts.length < 2) return false;

    return true;

}


export default function Register({ onSuccess }) {
  const [form, setForm] = useState({
    email: '',
    password: '',
    confirm: '',
    username: ''
  });
  const [localError, setLocalError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [emailValid, setEmailValid] = useState(false);

  const navigate = useNavigate();

  function handleChange(field) {
    
    return (e) => {
        if (field === 'email') {
            setEmailValid(validateEmail(e.target.value));
        }

      setForm((prev) => ({
        ...prev,
        [field]: e.target.value
      }));
    };
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setLocalError('');
    setSuccessMessage('');

    
    if (!form.email || !form.password || !form.confirm || !form.username) {
      setLocalError('请完整填写所有必填项');
      return;
    }
    if (!emailValid) {
        setLocalError('');
        return;
    }
    if (form.password !== form.confirm) {
      setLocalError('两次输入的密码不一致');
      return;
    }
    if (form.password.length < 6) {
      setLocalError('密码至少需要 6 位字符');
      return;
    }
    

    setSubmitting(true);
    const result = await createAccount({
      email: form.email,
      password: form.password,
      username: form.username.trim()
    });
    setSubmitting(false);

    if (result.success) {
      setSuccessMessage(result.message || '注册成功');
      setForm({ email: '', password: '', confirm: '', username: '' });
      onSuccess?.({ email: form.email });
      navigate('/');
    } else {
      setLocalError(result.message || '注册失败');
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="brand-bubble" aria-hidden />
          <h1 className="brand-title">创建账户</h1>
          <p className="brand-sub">注册以继续</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <div className="avatar-wrap" aria-hidden>
            <div className="avatar-clip">
              <img src="/image/profile.webp" alt="avatar" />
            </div>
          </div>

          <label className="field">
            <span className="label">邮箱</span>
            <input
              type="email"
              value={form.email}
              onChange={handleChange('email')}
              placeholder="you@example.com"
              className="input"
              required
            />
            {!emailValid && form.email && (
              <div className="error">请输入有效的邮箱地址</div>
            )}
          </label>

          <label className="field">
            <span className="label">昵称</span>
            <input
              type="text"
              value={form.username}
              onChange={(e) => {
                const value = e.target.value;
                if (value.includes('@')) {
                  return;
                }
                handleChange('username')(e);
            }}
              placeholder="username"
              className="input"
            />
          </label>

          <label className="field">
            <span className="label">密码</span>
            <input
              type="password"
              value={form.password}
              onChange={handleChange('password')}
              placeholder="至少 6 位"
              className="input"
              required
            />
          </label>

          <label className="field">
            <span className="label">确认密码</span>
            <input
              type="password"
              value={form.confirm}
              onChange={handleChange('confirm')}
              placeholder="再次输入密码"
              className="input"
              required
            />
          </label>

          {localError && <div className="error">{localError}</div>}
          {successMessage && <div className="success">{successMessage}</div>}

          <button className="btn primary" type="submit" disabled={submitting}>
            {submitting ? '提交中…' : '创建账户'}
          </button>
            <div className="row">
			    <button type="button" className="btn ghost"
			    onClick={() => navigate('/login')}
				>账户登录</button>
		    </div>
        </form>
      </div>
    </div>
  );
}
