import { useState } from 'react';
import './Login.css';
import { register } from '../API/auth';
import { useNavigate } from "react-router-dom";
import { AuthGlyph } from '../Main/LoginIcon/LoginIcon';

interface RegisterForm {
  email: string;
  password: string;
  confirm: string;
  username: string;
}

interface AccountInput {
  email: string;
  password: string;
  username: string;
}

interface RegisterSuccess {
  success: boolean;
  message: string;
}

async function createAccount({ email, password, username }: AccountInput): Promise<RegisterSuccess> {
  try {
    const params = {
        username: username,
        pass: password,
        email: email
    };

    const res = await register(params.email, params.pass, params.username);

    return { success: res.data.success, message: res.data.result };
  } catch (err) {
    console.error('注册请求失败', err);
    return { success: false, message: '网络错误' };
  }
}

function validateEmail(email: string): boolean {
    const address = email.split('@');

    if (address.length !== 2) return false;

    const domainParts = address[1].split('.');
    if (domainParts.length < 2) return false;

    return true;

}


export default function Register({ onSuccess }: { onSuccess?: (info: { email: string }) => void }) {
  const [form, setForm] = useState<RegisterForm>({
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

  function handleChange(field: string) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
        if (field === 'email') {
            setEmailValid(validateEmail(e.target.value));
        }

      setForm((prev) => ({
        ...prev,
        [field]: e.target.value
      }));
    };
  }

  async function handleSubmit(e: React.FormEvent) {
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
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="auth-glyph-circle" aria-hidden>
            <AuthGlyph loggedIn={false} />
          </div>
          <h1 className="brand-title">创建账户</h1>
          <p className="brand-sub">注册以继续</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label className="field">
            <span className="field-label">邮箱</span>
            <input
              type="email"
              value={form.email}
              onChange={handleChange('email')}
              placeholder="you@example.com"
              className="field-input"
              required
            />
            {!emailValid && form.email && (
              <div className="auth-error">请输入有效的邮箱地址</div>
            )}
          </label>

          <label className="field">
            <span className="field-label">昵称</span>
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
              className="field-input"
            />
          </label>

          <label className="field">
            <span className="field-label">密码</span>
            <input
              type="password"
              value={form.password}
              onChange={handleChange('password')}
              placeholder="至少 6 位"
              className="field-input"
              required
            />
          </label>

          <label className="field">
            <span className="field-label">确认密码</span>
            <input
              type="password"
              value={form.confirm}
              onChange={handleChange('confirm')}
              placeholder="再次输入密码"
              className="field-input"
              required
            />
          </label>

          {localError && <div className="auth-error">{localError}</div>}
          {successMessage && <div className="auth-success">{successMessage}</div>}

          <button className="btn primary" type="submit" disabled={submitting}>
            {submitting ? '提交中…' : '创建账户'}
          </button>
            <div className="auth-row">
			    <button type="button" className="btn ghost"
			    onClick={() => navigate('/login')}
				>账户登录</button>
		    </div>
        </form>
      </div>
    </div>
  );
}
