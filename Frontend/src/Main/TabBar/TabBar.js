import './TabBar.css';

export default function TabBar({ tabs, active, onSelect }) {
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
