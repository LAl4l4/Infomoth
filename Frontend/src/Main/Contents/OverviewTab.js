import './IntroPage.css';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  fetchSentimentScore,
  fetchAISkills,
  fetchExchangeRate,
  selectSentimentScore,
  selectAISkills,
  selectExchangeRates,
} from '../../Variable/dataCache';

function OverviewSentimentRow() {
  const dispatch = useDispatch();
  const { data: score, loading, error } = useSelector(selectSentimentScore);

  useEffect(() => {
    dispatch(fetchSentimentScore());
  }, [dispatch]);

  let valueText;
  if (loading) valueText = '加载中…';
  else if (error) valueText = '暂无';
  else if (score === null) valueText = '今日暂无';
  else valueText = `${score > 0 ? '+' : ''}${score.toFixed(4)}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">市场情绪</span>
      <span className={'snapshot-value' + (score === null && !loading && !error ? ' muted' : '')}>
        {valueText}
      </span>
    </div>
  );
}

function OverviewAISkillRow() {
  const dispatch = useDispatch();
  const { data: skills, loading } = useSelector(selectAISkills);

  useEffect(() => {
    dispatch(fetchAISkills());
  }, [dispatch]);

  const top = skills && skills.length > 0 ? skills[0] : null;

  let valueText;
  if (loading) valueText = '加载中…';
  else if (!top) valueText = '今日暂无';
  else valueText = `${top.rank}. ${top.skill}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">AI 热门技能 Top 1</span>
      <span className={'snapshot-value' + (!top ? ' muted' : '')}>{valueText}</span>
    </div>
  );
}

function OverviewRateRow() {
  const dispatch = useDispatch();
  const { data: rates, loading } = useSelector(selectExchangeRates);

  useEffect(() => {
    dispatch(fetchExchangeRate({ base: 'USD', quote: 'CNY' }));
  }, [dispatch]);

  const rate = rates['USD-CNY'];

  let valueText;
  if (loading) valueText = '加载中…';
  else if (rate === undefined || rate === null) valueText = '今日暂无';
  else valueText = `1 USD = ${rate} CNY`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">汇率速查 (USD/CNY)</span>
      <span className={'snapshot-value' + (rate === undefined || rate === null ? ' muted' : '')}>{valueText}</span>
    </div>
  );
}

export default function OverviewTab() {
  return (
    <section className="glass-panel" aria-label="概览">
      <p className="eyebrow">Overview</p>
      <h2 className="panel-title">今日资讯概览</h2>
      <p className="panel-lead">
        欢迎来到 InfoMoth。这里汇聚当日市场情绪、AI 热门技能与汇率速查，
        在顶部的标签栏中切换即可深入查看每一项数据。
      </p>

      <div className="overview-snapshot">
        <OverviewSentimentRow />
        <OverviewAISkillRow />
        <OverviewRateRow />
      </div>

      <p className="overview-foot">
        数据由后端定时抓取与更新，如需详细解读请切换到对应标签页。
      </p>
    </section>
  );
}
