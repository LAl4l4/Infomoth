import {useState, useEffect} from "react";
import './PgCtrl.css';
import Intro from './Main/Frontpage/IntroPage';

export default function PageCtrl({isup}) {
    const [pagenum, setnum] = useState(0);

    if (pagenum < 0) {
        setnum(0);
    } else if (pagenum > 3) {
        setnum(3);
    }


    return (
        <div id={"background" + (isup? "up" : "")}
        >
            <Orbs visible={isup} pagenum={pagenum} />
            <PageArrow
                onNext={()=>setnum(pagenum-1)}
                onPrev={()=>setnum(pagenum+1)}
            />
            <Intro pagenum={pagenum}/>
            
        </div>
    );
}

function Orbs({visible, pagenum}){
    const [boost, setboost] = useState(0);
    const [prevPage, setPrevPage] = useState(pagenum);

    useEffect(() => {
        const diff = pagenum - prevPage;

        if (diff > 0) {
            // 翻页向前 → 顺时针
            setboost(prev => prev + 90 + 25 * diff);
        } else if (diff < 0) {
            // 翻页向后 → 逆时针
            setboost(prev => prev - 90 + 25 * diff);
        }

        setPrevPage(pagenum); // 更新上一次页码
    }, [pagenum, prevPage]);
    // --offset is used to rotate the whole orbital system when page changes
    const wrapStyle = { '--offset': `${boost}deg` };
    return (
        <div className={"orb-wrap" + (visible? " visible" : "")} style={wrapStyle}>
            <div className="orbit orbit-1"><div className="orb"/></div>
            <div className="orbit orbit-2"><div className="orb"/></div>
            <div className="orbit orbit-3"><div className="orb"/></div>
            <div className="orbit orbit-4"><div className="orb"/></div>
            <div className="orbit orbit-5"><div className="orb"/></div>
        </div>
    );
}

function PageArrow({onNext, onPrev}) {
    return (
        <div>
            <img
                id="uparrow"
                src="image/whArrowup.png"
                alt="prev"
                onClick={()=>onNext()}
            />
            <img
                id="downarrow"
                src="image/whArrowdown.png"
                alt="next"
                onClick={()=>onPrev()}
            />
        </div>
    );
}