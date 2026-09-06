export default function RefreshStatus({ updatedAt, onRefresh }: {
  updatedAt?: number;
  onRefresh: () => void;
}) {
  return (
    <div className="state-text">
      {updatedAt ? `最近获取：${new Date(updatedAt).toLocaleString()}` : '尚未成功获取数据'}
      {' · '}<button type="button" onClick={onRefresh}>刷新数据</button>
    </div>
  );
}
