import './LoginIcon.css';
import {useState, useRef} from 'react';
import { useNavigate } from "react-router-dom";
import { useSelector, useDispatch } from 'react-redux';
import { selectIsLoggedIn, logOut } from '../../Variable/login';

export default function Icon({open}) {
    const [hover, setHover] = useState(false);
    const hideTimer = useRef(null);
    const isLoggedIn = useSelector(selectIsLoggedIn);
    const dispatch = useDispatch();
    const navigate = useNavigate();

    
    function handleEnter() {
        // cancel hide timer if coming back
        clearTimeout(hideTimer.current);
        setHover(true);
    }

    function handleLeave() {
        // schedule delayed closing
        hideTimer.current = setTimeout(() => {
            setHover(false);
        }, 200); // 👈调这个时间
    }

    function handleLogout() {
        dispatch(logOut());
        navigate('/');
    }

    return (
        <div
            className={"login-icon-container" + (open ? "" : " center")}
            onMouseEnter={()=> open && handleEnter()}
            onMouseLeave={()=> open && handleLeave()}
            aria-haspopup="true"
        >
            <div className="icon-clip">
                <img
                    src="/image/profile.webp"
                    className="blog-icon" 
                    alt="profile"
                />
            </div>

            <div className={"icon-dropdown" + (hover ? " visible" : "")} role="menu">
                {!isLoggedIn ? (
                    <>
                        <button 
                            className="dropdown-item"
                            onClick={()=>navigate("/login")}
                        >Login</button>
                        <button 
                            className="dropdown-item"
                            onClick={()=>navigate("/register")}
                        >Register</button>
                    </>
                ) : (
                    <>
                        <button 
                            className="dropdown-item"
                            onClick={()=>navigate("/profile")}
                        >Profile</button>
                        <button 
                            className="dropdown-item"
                            onClick={()=>navigate("/settings")}
                        >Settings</button>
                        <button 
                            className="dropdown-item"
                            onClick={handleLogout}
                        >Logout</button>
                    </>
                )}
            </div>
        </div>
    );
}