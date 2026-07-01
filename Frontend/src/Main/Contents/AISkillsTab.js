import './IntroPage.css';
import { useEffect, useState } from 'react';
import { pullPopularAISkills } from '../../API/data';

export default function AISkillsTab() {
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    pullPopularAISkills()
      .then((items) => mounted && setSkills(items))
      .catch((e) => mounted && setError(e.message || 'AI 技能数据加载失败'))
      .finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, []);

  return (
    <section className="glass-panel" aria-label="AI 热门技能">
      <p className="eyebrow">AI Skills</p>
      <h2 className="panel-title">AI 热门技能榜</h2>
      <p className="panel-lead">
        今日被讨论最多的 AI 技能，按提及量排序。
      </p>

      {loading && <p className="state-text">加载中…</p>}
      {!loading && error && <p className="exchange-error">{error}</p>}
      {!loading && !error && skills.length === 0 && (
        <p className="state-text">今天暂无可用数据。</p>
      )}
      {!loading && !error && skills.length > 0 && (
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
