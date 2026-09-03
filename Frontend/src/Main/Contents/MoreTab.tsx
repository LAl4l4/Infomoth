import './IntroPage.css';
import { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { fetchAISkills, selectAISkills } from '../../Variable/dataCache';
import type { AppDispatch } from '../../customTypes';

const COLLAPSED_SKILL_COUNT = 5;

export default function MoreTab() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: skills, loading, error } = useSelector(selectAISkills);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    dispatch(fetchAISkills());
  }, [dispatch]);

  const visibleSkills = expanded ? skills : skills?.slice(0, COLLAPSED_SKILL_COUNT);
  const skillCount = skills?.length ?? 0;
  const canExpand = skillCount > COLLAPSED_SKILL_COUNT;

  return (
    <section className="glass-panel" aria-label="更多内容">
      <p className="eyebrow">More</p>
      <h2 className="panel-title">更多内容</h2>
      <p className="panel-lead">
        收纳不需要频繁查看的补充信息与趋势。
      </p>

      <div className="more-section">
        <div className="more-section-header">
          <div>
            <p className="more-section-kicker">AI Skills</p>
            <h3 className="more-section-title">AI 热门技能榜</h3>
            <p className="more-section-description">
              今日被讨论最多的 AI 技能，按提及量排序。
            </p>
          </div>
          <span className="more-section-badge">每日更新</span>
        </div>

        {loading && <p className="state-text">加载中…</p>}
        {!loading && error && <p className="exchange-error">{error}</p>}
        {!loading && !error && (!skills || skills.length === 0) && (
          <p className="state-text">今天暂无可用数据。</p>
        )}
        {!loading && !error && visibleSkills && visibleSkills.length > 0 && (
          <>
            <ol className="skill-list">
              {visibleSkills.map((item) => (
                <li key={`${item.rank}-${item.skill}`} className="skill-row">
                  <span className="skill-rank">{String(item.rank).padStart(2, '0')}</span>
                  <span className="skill-name">{item.skill}</span>
                  <span className="skill-meta">{item.mentions} mentions</span>
                </li>
              ))}
            </ol>
            {canExpand && (
              <button
                type="button"
                className="skill-expand"
                aria-expanded={expanded}
                onClick={() => setExpanded((value) => !value)}
              >
                {expanded ? '收起榜单' : `查看全部 ${skillCount} 项`}
              </button>
            )}
          </>
        )}
      </div>
    </section>
  );
}
