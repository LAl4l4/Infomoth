import {useState} from 'react';
import './Login.css';
import { useNavigate } from "react-router-dom";
import { useDispatch } from 'react-redux';
import { setPageNum } from '../Variable/pagenum';
import { checkLoginThunk } from '../Variable/login';


// Front-end only shell. The app's backend integration should pass an `onSubmit` prop.
// Example usage:
// <Login onSubmit={(creds)=> api.login(creds).then(...)} />


export default function Login() {
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [localError, setLocalError] = useState('');
	const navigate = useNavigate();
	const dispatch = useDispatch();

	async function handleSubmit(e) {
		e.preventDefault();
		setLocalError('');

		const resultAction = await dispatch(
			checkLoginThunk({ email, password })
		);

		if (checkLoginThunk.fulfilled.match(resultAction)) {
			alert('登录成功');
			dispatch(setPageNum(0));
			navigate('/');
		} else {
			setLocalError(resultAction.payload);
		}
	}



	return (
		<div className="login-page">
			<div className="login-card">
				<div className="login-brand">
					<div className="brand-bubble" aria-hidden />
					<h1 className="brand-title">欢迎回来</h1>
					<p className="brand-sub">使用你的账户登录以继续</p>
				</div>

				<form className="login-form" onSubmit={handleSubmit} noValidate>
					<div className="avatar-wrap" aria-hidden>
						<div className="avatar-clip">
							<img src="/image/profile.webp" alt="avatar" />
						</div>
					</div>

					<label className="field">
						<span className="label">邮箱/用户名</span>
						<input
							type="text"
							value={email}
							onChange={e=>setEmail(e.target.value)}
							placeholder="yours@example.com/username"
							className="input"
							required
						/>
					</label>

					<label className="field">
						<span className="label">密码</span>
						<input
							type="password"
							value={password}
							onChange={e=>setPassword(e.target.value)}
							placeholder="password"
							className="input"
							required
						/>
					</label>

					{localError && <div className="error">{localError}</div>}

					<button className="btn primary" type="submit">
						登录
					</button>

					<div className="row">
						<button type="button" className="btn ghost"
						onClick={() => navigate('/register')}
						>账号注册</button>
						<a className="link" href="#forgot">忘记密码?</a>
					</div>
				</form>
			</div>
		</div>
	);
}
