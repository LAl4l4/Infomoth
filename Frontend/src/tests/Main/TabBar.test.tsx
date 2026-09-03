import { render, screen, fireEvent } from '@testing-library/react';
import TabBar from '../../Main/TabBar/TabBar';

const TABS = [
  { key: 0, label: '概览' },
  { key: 1, label: '市场情绪' },
  { key: 2, label: '更多' },
];

describe('TabBar', () => {
  it('renders all tabs with tablist semantics', () => {
    render(<TabBar tabs={TABS} active={0} onSelect={() => {}} />);

    expect(screen.getByRole('tablist')).toBeInTheDocument();
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(3);
    expect(tabs.map((t) => t.textContent)).toEqual(['概览', '市场情绪', '更多']);
  });

  it('marks only the active tab as selected', () => {
    render(<TabBar tabs={TABS} active={1} onSelect={() => {}} />);

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'false');
    expect(tabs[1]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[1].className).toContain('active');
    expect(tabs[0].className).not.toContain(' active');
  });

  it('calls onSelect with the tab key when clicked', () => {
    const onSelect = jest.fn();
    render(<TabBar tabs={TABS} active={0} onSelect={onSelect} />);

    fireEvent.click(screen.getByText('更多'));
    expect(onSelect).toHaveBeenCalledWith(2);
  });
});
