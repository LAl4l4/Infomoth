import {useState, useEffect} from "react";
import { useSelector, useDispatch } from 'react-redux';
import { selectPageNum, setPageNum } from '../../Variable/pagenum';
import './PgCtrl.css';
import Intro from './IntroPage';

export default function PageCtrl({isup}) {
    const pagenum = useSelector(selectPageNum);
    const dispatch = useDispatch();
    const [loadarrow, setLoadarrow] = useState(false);

    // Clamp pagenum within bounds when it changes
    useEffect(() => {
        if (pagenum < 0) {
            dispatch(setPageNum(0));
        } else if (pagenum > 3) {
            dispatch(setPageNum(3));
        }
    }, [pagenum, dispatch]);

    useEffect(() => {
        let timer;

        if (isup) {
        // 背景上升动画时长假设是 0.6s + 0.3s delay
        timer = setTimeout(() => setLoadarrow(true), 600); // 0.9s 后显示箭头
        } else {
        // 收回背景时隐藏箭头
        setLoadarrow(false);
        }

        return () => clearTimeout(timer);
    }, [isup]);

    return (
        <div id={"background" + (isup? "up" : "")}
        >
            <Orbs visible={isup} pagenum={pagenum} />
            {loadarrow && (
                <PageArrow
                    onNext={() => dispatch(setPageNum(pagenum - 1))}
                    onPrev={() => dispatch(setPageNum(pagenum + 1))}
                />
            )}
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
        <div className="page-arrows">
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