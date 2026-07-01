import './LoginIcon.css';
import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { selectIsLoggedIn, logOut } from '../../Variable/login';

export default function Icon() {
  const [hover, setHover] = useState(false);
  const hideTimer = useRef(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  function handleEnter() {
    clearTimeout(hideTimer.current);
    setHover(true);
  }

  function handleLeave() {
    hideTimer.current = setTimeout(() => {
      setHover(false);
    }, 200);
  }

  function handleLogout() {
    dispatch(logOut());
    navigate('/');
  }

  return (
    <div
      className="auth-icon-wrap"
      onMouseEnter={handleEnter}
      onMouseLeave={handleLeave}
      aria-haspopup="true"
    >
      <button className="auth-glass-icon" aria-label="账户">
        <AuthGlyph loggedIn={isLoggedIn} />
      </button>

      <div className={'auth-dropdown' + (hover ? ' visible' : '')} role="menu">
        {!isLoggedIn ? (
          <>
            <button
              className="auth-dropdown-item"
              onClick={() => navigate('/login')}
            >Login</button>
            <button
              className="auth-dropdown-item"
              onClick={() => navigate('/register')}
            >Register</button>
          </>
        ) : (
          <>
            <button
              className="auth-dropdown-item"
              onClick={() => navigate('/profile')}
            >Profile</button>
            <button
              className="auth-dropdown-item"
              onClick={() => navigate('/settings')}
            >Settings</button>
            <button
              className="auth-dropdown-item"
              onClick={handleLogout}
            >Logout</button>
          </>
        )}
      </div>
    </div>
  );
}

/* Minimal inline SVG: a person silhouette + a small lock badge to signal
   that this is an auth / account entry. Exported so Login/Register pages
   can reuse the same glyph inside their own glass circles. */
export function AuthGlyph({ loggedIn }) {
  return (
    <svg viewBox="0 0 32 32" className="auth-glyph" aria-hidden>
      <circle cx="16" cy="12" r="5.2" fill="none" stroke="currentColor" strokeWidth="2.2" />
      <path
        d="M6.5 27c1.6-4.8 5.4-7.4 9.5-7.4S23.9 22.2 25.5 27"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      {loggedIn ? (
        <circle cx="25" cy="25" r="5.5" fill="rgba(34,197,94,0.85)" stroke="#fff" strokeWidth="1.6" />
      ) : (
        <g transform="translate(21.5,21.5)">
          <rect x="0" y="2.2" width="7" height="5.6" rx="1.2" fill="rgba(15,23,42,0.85)" stroke="#fff" strokeWidth="1" />
          <path d="M1.4 2.4 V1.4 a2.1 2.1 0 0 1 4.2 0 V2.4" fill="none" stroke="rgba(15,23,42,0.85)" strokeWidth="1.4" />
        </g>
      )}
    </svg>
  );
}
