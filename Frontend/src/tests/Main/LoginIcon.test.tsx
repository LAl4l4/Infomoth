import { screen, fireEvent, act } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import LoginIcon from '../../Main/LoginIcon/LoginIcon';
import { renderWithProviders } from '../testUtils';

describe('LoginIcon', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows Login/Register menu when logged out', () => {
    renderWithProviders(<LoginIcon />);
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Register' })).toBeInTheDocument();
  });

  it('shows Profile/Settings/Logout menu when logged in', () => {
    const { store } = renderWithProviders(<LoginIcon />);
    act(() => { store.dispatch({ type: 'login/logIn' }); });

    expect(screen.getByRole('button', { name: 'Profile' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Logout' })).toBeInTheDocument();
  });

  it('dropdown becomes visible on hover and hides after leaving', () => {
    jest.useFakeTimers();
    const { container } = renderWithProviders(<LoginIcon />);
    const wrap = container.querySelector('.auth-icon-wrap')!;
    const dropdown = container.querySelector('.auth-dropdown')!;

    expect(dropdown.className).not.toContain('visible');

    fireEvent.mouseEnter(wrap);
    expect(dropdown.className).toContain('visible');

    fireEvent.mouseLeave(wrap);
    act(() => { jest.advanceTimersByTime(250); });
    expect(dropdown.className).not.toContain('visible');
    jest.useRealTimers();
  });

  it('logout clears login state and the stored token', () => {
    localStorage.setItem('authToken', 'tok');
    const { store } = renderWithProviders(<LoginIcon />);
    act(() => { store.dispatch({ type: 'login/logIn' }); });

    fireEvent.click(screen.getByRole('button', { name: 'Logout' }));

    expect(store.getState().login.isLoggedIn).toBe(false);
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it('navigates to /login when Login is clicked', () => {
    renderWithProviders(
      <Routes>
        <Route path="/" element={<LoginIcon />} />
        <Route path="/login" element={<div>登录页</div>} />
      </Routes>
    );
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));
    expect(screen.getByText('登录页')).toBeInTheDocument();
  });

  it('navigates to /register when Register is clicked', () => {
    renderWithProviders(
      <Routes>
        <Route path="/" element={<LoginIcon />} />
        <Route path="/register" element={<div>注册页</div>} />
      </Routes>
    );
    fireEvent.click(screen.getByRole('button', { name: 'Register' }));
    expect(screen.getByText('注册页')).toBeInTheDocument();
  });
});
