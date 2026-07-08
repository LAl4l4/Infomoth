import './PageShell.css';
import { useSelector, useDispatch } from 'react-redux';
import { selectPageNum, setPageNum } from '../../Variable/pagenum';
import LoginIcon from '../LoginIcon/LoginIcon';
//import { BgGlobe } from '../BackgroundGlobe/BackgroundGlobe';
import Globe from '../BackgroundGlobe/Globe';
import TabBar from '../TabBar/TabBar';
import OverviewTab from '../Contents/OverviewTab';
import SentimentTab from '../Contents/SentimentTab';
import AISkillsTab from '../Contents/AISkillsTab';
import ExchangeRateTab from '../Contents/ExchangeRateTab';
import UsStockTab from '../Contents/UsStockTab';
import type { AppDispatch, TabItem } from '../../customTypes';

const TABS: TabItem[] = [
  { key: 0, label: '概览' },
  { key: 1, label: '市场情绪' },
  { key: 2, label: 'AI技能' },
  { key: 3, label: '汇率' },
  { key: 4, label: '美股' },
];

export default function PageShell() {
  const pagenum = useSelector(selectPageNum);
  const dispatch = useDispatch<AppDispatch>();

  const safeTab = Math.min(Math.max(pagenum, 0), 4);

  return (
    <div className="page-shell">
      <Globe />
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
          {safeTab === 4 && <UsStockTab />}
        </div>
      </div>
    </div>
  );
}
