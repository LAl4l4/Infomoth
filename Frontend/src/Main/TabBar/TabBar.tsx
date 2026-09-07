import './TabBar.css';
import { useEffect, useRef } from 'react';
import type { TabItem } from '../../customTypes';

interface TabBarProps {
  tabs: TabItem[];
  active: number;
  onSelect: (key: number) => void;
}

export default function TabBar({ tabs, active, onSelect }: TabBarProps) {
  const bar = useRef<HTMLDivElement>(null);
  useEffect(() => {
    // Keep the selected tab visible when the seven-tab bar overflows on mobile.
    bar.current?.querySelector('[aria-selected="true"]')?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  }, [active]);
  return (
    <div className="tabbar-wrap">
      <div className="tabbar" role="tablist" ref={bar}>
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
