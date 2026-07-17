import { BrowserRouter, Routes, Route } from "react-router-dom";
import './App.css';
import PageShell from './Main/PageShell/PageShell';
import Login from './Account/Login';
import Register from './Account/Register';
import Profile from './Account/Profile';
import Settings from './Account/Settings';

function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<PageShell />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
