import './Frontpage.css';
import { useSelector, useDispatch } from 'react-redux';
import { selectIsOpen, toggleIsOpen } from '../../Variable/isopen';
import PageCtrl from '../Contents/PageController';
import LoginIcon from '../LoginIcon/LoginIcon';



export default function Frontpage() {
    const isOpen = useSelector(selectIsOpen);
    const dispatch = useDispatch();

    return (
        <div className='frontpage-background'>
            
            <FrontImg 
                open={isOpen}
                setopen={()=>dispatch(toggleIsOpen())}
            />
            <LoginIcon
                open={isOpen}
            />
            <PageCtrl 
                isup={isOpen}
            />

            
        </div>
    );
}

function FrontImg({open, setopen}) {

    return (
        <div 
            className={"frontpage-container" + (open ? "slide" : "")}
            onClick = {()=>setopen()}
        >
            <img
            src="image/backgroundtwo.png"
            className="frontpage-img" 
            alt="front"

            />
            <FrontArrow />
            {/* cover wave: a neat ruler-like scallop edge (static). white below, transparent above, shadow below */}
            <div className={"cover-wave visible"} aria-hidden>
                <svg viewBox="0 0 1440 80" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                        {/* shadow filter for the duplicated shadow path */}
                        <filter id="downShadow" x="-50%" y="-50%" width="200%" height="200%">
                            {/* blur the alpha */}
                            <feGaussianBlur in="SourceAlpha" stdDeviation="6" result="blur"/>
                            {/* offset downward */}
                            <feOffset in="blur" dx="0" dy="6" result="offsetBlur"/>
                            {/* color the blurred offset */}
                            <feFlood flood-color="#000" floodOpacity="0.2" result="color"/>
                            <feComposite in="color" in2="offsetBlur" operator="in" result="shadow"/>
                            <feMerge>
                                <feMergeNode in="shadow"/>
                                <feMergeNode in="SourceGraphic"/>
                            </feMerge>
                        </filter>
                    </defs>
                    
                    {/* main white scallop on top (no filter) */}
                    <path
                        d="M0,60
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            L1440,180 L0,180 Z"
                        fill="#ffffff"
                        filter="url(#downShadow)"
                    />
                    {/* repeated semicircles to form a ruler-like edge */}
                    {/* shadow path: same shape, shifted down slightly, blurred by filter */}
                    <path
                        id="shadowpath"
                        d="M0,60
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            a60,60 0 0,1 120,0
                            L1440,140 L0,140 Z"
                        transform="translate(0,0)"
                        fill="#000"
                        opacity="0.05"
                        filter="url(#downShadow)"
                    />
                </svg>
            </div>
        </div>
    )
}

function FrontArrow() {
    return (
        <div className="arrow-container">
            {/* 静止箭头 */}
            <img    
                src="image/whArrowup.png"
                className="arrow-static"
                alt="front"
            />

            {/* 动画箭头：循环出现并往上漂 */}
            <div className='floater'>
                <img    
                    src="image/whArrowup.png"
                    className="arrow-floating"
                    alt="floating"
                />
            </div>
        </div>
    )
}
