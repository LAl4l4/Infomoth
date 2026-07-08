import './IntroPage.css';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { fetchAISkills, selectAISkills } from '../../Variable/dataCache';
import type { AppDispatch } from '../../customTypes';

export default function AISkillsTab() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: skills, loading, error } = useSelector(selectAISkills);

  useEffect(() => {
    dispatch(fetchAISkills());
  }, [dispatch]);

  return (
    <section className="glass-panel" aria-label="AI 热门技能">
      <p className="eyebrow">AI Skills</p>
      <h2 className="panel-title">AI 热门技能榜</h2>
      <p className="panel-lead">
        今日被讨论最多的 AI 技能，按提及量排序。
      </p>

      {loading && <p className="state-text">加载中…</p>}
      {!loading && error && <p className="exchange-error">{error}</p>}
      {!loading && !error && (!skills || skills.length === 0) && (
        <p className="state-text">今天暂无可用数据。</p>
      )}
      {!loading && !error && skills && skills.length > 0 && (
        <ol className="skill-list">
          {skills.map((item) => (
            <li key={`${item.rank}-${item.skill}`} className="skill-row">
              <span className="skill-rank">{String(item.rank).padStart(2, '0')}</span>
              <span className="skill-name">{item.skill}</span>
              <span className="skill-meta">{item.mentions} mentions</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
