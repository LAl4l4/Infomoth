import { BrowserRouter, Routes, Route } from "react-router-dom";
import './App.css';
import Frontpage from './Main/Frontpage/frontpage';
import Login from './Account/Login';
import Register from './Account/Register';
import Profile from './Account/Profile';

function App() {
  return (
    <div className="App">
      <header className="App-header">


      </header>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Frontpage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/profile" element={<Profile />} />

        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
