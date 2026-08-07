import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import type { AppDispatch } from '../customTypes';
import { pullCurrencies } from '../API/data';
import { pullGeneralSettings, updateGeneralSettings } from '../API/settings';
import { setPageNum } from '../Variable/pagenum';
import './Settings.css';

type SettingsTab = 'general' | 'display' | 'about';

interface LeftSectionProps {
  tab: SettingsTab;
  setTab: (tab: SettingsTab) => void;
}

interface GeneralSettingsProps {
  onOpenHome: (page: number) => void;
}

const TABS: { key: SettingsTab; label: string }[] = [
  { key: 'general', label: '常规' },
  { key: 'display', label: '显示' },
  { key: 'about', label: '关于' },
];

const HOME_PAGE_OPTIONS = [
  { value: 0, label: '概览' },
  { value: 1, label: '市场情绪' },
  { value: 2, label: 'AI 技能' },
  { value: 3, label: '汇率' },
  { value: 4, label: '美股' },
  { value: 5, label: '市场走势' },
];

export default function Settings() {
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const [tab, setTab] = useState<SettingsTab>('general');

  const openHomePage = (page: number) => {
    dispatch(setPageNum(page));
    navigate('/');
  };

  return (
    <div className="settings-screen">
      <div className="settings-header">
        <div className="back-arrow" onClick={() => navigate(-1)} title="返回">←</div>
        <div className="settings-title-wrap">
          <h1 className="settings-title">设置</h1>
          <p className="settings-sub">管理你的应用偏好</p>
        </div>
      </div>

      <div className="settings-body">
        <LeftSection tab={tab} setTab={setTab} />

        <div className="settings-right">
          {tab === 'general' && <GeneralSettings onOpenHome={openHomePage} />}
          {tab === 'display' && <DisplaySettings />}
          {tab === 'about' && <AboutSettings />}
        </div>
      </div>
    </div>
  );
}

function LeftSection({ tab, setTab }: LeftSectionProps) {
  return (
    <div className="leftsection">
      <div className="left-section">
        {TABS.map(({ key, label }) => (
          <h2
            key={key}
            className={'card-title' + (tab === key ? ' active' : '')}
            onClick={() => setTab(key)}
          >
            {label}
          </h2>
        ))}
      </div>
    </div>
  );
}

function GeneralSettings({ onOpenHome }: GeneralSettingsProps) {
  const [defaultPage, setDefaultPage] = useState(0);
  const [defaultBaseCurrency, setDefaultBaseCurrency] = useState('USD');
  const [defaultQuoteCurrency, setDefaultQuoteCurrency] = useState('CNY');
  const [currencies, setCurrencies] = useState<string[]>(['USD', 'CNY']);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [status, setStatus] = useState('正在读取账户设置…');

  useEffect(() => {
    let active = true;

    const loadSettings = async () => {
      try {
        const settings = await pullGeneralSettings();
        if (!active) return;
        setDefaultPage(settings.defaultPage);
        const baseCurrency = settings.defaultBaseCurrency || 'USD';
        const quoteCurrency = settings.defaultQuoteCurrency || 'CNY';
        setDefaultBaseCurrency(baseCurrency);
        setDefaultQuoteCurrency(quoteCurrency);
        setCurrencies((current) => Array.from(new Set([
          ...current,
          baseCurrency,
          quoteCurrency,
        ])).sort());
        setStatus('设置已同步');
      } catch (error: unknown) {
        if (!active) return;
        setStatus(error instanceof Error ? error.message : '读取设置失败');
      } finally {
        if (active) setIsLoading(false);
      }
    };

    void loadSettings();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    pullCurrencies()
      .then((result) => {
        if (active) setCurrencies(Array.from(new Set(result)).sort());
      })
      .catch(() => {
        // Keep the built-in USD/CNY options when the currency list is unavailable.
      });

    return () => {
      active = false;
    };
  }, []);

  const saveSettings = async (
    page: number,
    baseCurrency: string,
    quoteCurrency: string,
    returnHome = false
  ) => {
    setIsSaving(true);
    setStatus('正在保存…');

    try {
      const settings = await updateGeneralSettings(page, baseCurrency, quoteCurrency);
      const savedPage = settings.defaultPage ?? page;
      setDefaultPage(savedPage);
      setDefaultBaseCurrency(settings.defaultBaseCurrency || baseCurrency);
      setDefaultQuoteCurrency(settings.defaultQuoteCurrency || quoteCurrency);
      setStatus('已保存到你的账户');
      if (returnHome) onOpenHome(savedPage);
    } catch (error: unknown) {
      setStatus(error instanceof Error ? error.message : '保存设置失败');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="right-inner">
      <div className="card-block">
        <h3 className="section-title">常规</h3>
        <div className="setting-group">
          <div className="setting-copy">
            <label className="setting-label" htmlFor="default-home-page">默认首页</label>
            <p className="setting-description">下次打开 InfoMoth 时优先显示的栏目。</p>
          </div>
          <select
            id="default-home-page"
            className="setting-select"
            value={defaultPage}
            disabled={isLoading || isSaving}
            onChange={(event) => {
              setDefaultPage(Number(event.target.value));
              setStatus('有未保存的更改');
            }}
          >
            {HOME_PAGE_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>

        <div className="setting-group">
          <div className="setting-copy">
            <label className="setting-label" htmlFor="default-base-currency">默认基础货币</label>
            <p className="setting-description">汇率查询左侧的基础货币。</p>
          </div>
          <select
            id="default-base-currency"
            className="setting-select"
            value={defaultBaseCurrency}
            disabled={isLoading || isSaving}
            onChange={(event) => {
              setDefaultBaseCurrency(event.target.value);
              setStatus('有未保存的更改');
            }}
          >
            {currencies.map((currency) => (
              <option key={`base-${currency}`} value={currency}>{currency}</option>
            ))}
          </select>
        </div>

        <div className="setting-group">
          <div className="setting-copy">
            <label className="setting-label" htmlFor="default-quote-currency">默认目标货币</label>
            <p className="setting-description">汇率查询右侧的目标货币。</p>
          </div>
          <select
            id="default-quote-currency"
            className="setting-select"
            value={defaultQuoteCurrency}
            disabled={isLoading || isSaving}
            onChange={(event) => {
              setDefaultQuoteCurrency(event.target.value);
              setStatus('有未保存的更改');
            }}
          >
            {currencies.map((currency) => (
              <option key={`quote-${currency}`} value={currency}>{currency}</option>
            ))}
          </select>
        </div>

        <div className="settings-actions">
          <span className="settings-save-hint" role="status">{status}</span>
          <div className="settings-action-buttons">
            <button
              className="settings-button secondary"
              type="button"
              disabled={isLoading || isSaving}
              onClick={() => saveSettings(0, 'USD', 'CNY')}
            >
              恢复默认
            </button>
            <button
              className="settings-button primary"
              type="button"
              disabled={isLoading || isSaving}
              onClick={() => saveSettings(
                defaultPage,
                defaultBaseCurrency,
                defaultQuoteCurrency,
                true
              )}
            >
              保存并返回首页
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function DisplaySettings() {
  return (
    <div className="right-inner">
      <div className="card-block">
        <h3 className="section-title">显示</h3>
        <div className="settings-placeholder">主题 / 动画 / 字号（待实现）</div>
      </div>
    </div>
  );
}

function AboutSettings() {
  return (
    <div className="right-inner">
      <div className="card-block">
        <h3 className="section-title">关于</h3>
        <div className="settings-placeholder">InfoMoth · v0.1.0</div>
      </div>
    </div>
  );
}
