import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import type { AppDispatch } from '../customTypes';
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

  const saveDefaultPage = async (page: number, returnHome = false) => {
    setIsSaving(true);
    setStatus('正在保存…');

    try {
      const settings = await updateGeneralSettings(page);
      setDefaultPage(settings.defaultPage);
      setStatus('已保存到你的账户');
      if (returnHome) onOpenHome(settings.defaultPage);
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

        <div className="settings-actions">
          <span className="settings-save-hint" role="status">{status}</span>
          <div className="settings-action-buttons">
            <button
              className="settings-button secondary"
              type="button"
              disabled={isLoading || isSaving}
              onClick={() => saveDefaultPage(0)}
            >
              恢复默认
            </button>
            <button
              className="settings-button primary"
              type="button"
              disabled={isLoading || isSaving}
              onClick={() => saveDefaultPage(defaultPage, true)}
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
