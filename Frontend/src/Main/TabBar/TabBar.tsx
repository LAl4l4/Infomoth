import './TabBar.css';
import type { TabItem } from '../../customTypes';

interface TabBarProps {
  tabs: TabItem[];
  active: number;
  onSelect: (key: number) => void;
}

export default function TabBar({ tabs, active, onSelect }: TabBarProps) {
  return (
    <div className="tabbar-wrap">
      <div className="tabbar" role="tablist">
        {tabs.map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={active === t.key}
            className={'tabbar-tab' + (active === t.key ? ' active' : '')}
            onClick={() => onSelect(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>
    </div>
  );
}
