import './PageShell.css';
import { useSelector, useDispatch } from 'react-redux';
import { selectPageNum, setPageNum } from '../../Variable/pagenum';
import LoginIcon from '../LoginIcon/LoginIcon';
import RainbowBubble from '../RainbowBubble/RainbowBubble';
import TabBar from '../TabBar/TabBar';
import OverviewTab from '../Contents/OverviewTab';
import SentimentTab from '../Contents/SentimentTab';
import AISkillsTab from '../Contents/AISkillsTab';
import ExchangeRateTab from '../Contents/ExchangeRateTab';

const TABS = [
  { key: 0, label: '概览' },
  { key: 1, label: '市场情绪' },
  { key: 2, label: 'AI技能' },
  { key: 3, label: '汇率' },
];

export default function PageShell() {
  const pagenum = useSelector(selectPageNum);
  const dispatch = useDispatch();

  const safeTab = Math.min(Math.max(pagenum, 0), 3);

  return (
    <div className="page-shell">
      <RainbowBubble />
      <TabBar
        tabs={TABS}
        active={safeTab}
        onSelect={(k) => dispatch(setPageNum(k))}
      />
      <LoginIcon />
      <div className="tab-content-shell">
        <div className="tab-content-inner">
          {safeTab === 0 && <OverviewTab />}
          {safeTab === 1 && <SentimentTab />}
          {safeTab === 2 && <AISkillsTab />}
          {safeTab === 3 && <ExchangeRateTab />}
        </div>
      </div>
    </div>
  );
}
