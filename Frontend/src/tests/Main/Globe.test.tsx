import { render } from '@testing-library/react';
import Globe from '../../Main/BackgroundGlobe/Globe';

const mockDestroy = jest.fn();
const mockCreateGlobe = jest.fn((..._args: unknown[]) => ({ destroy: mockDestroy }));

jest.mock('cobe', () => ({
  __esModule: true,
  default: (...args: unknown[]) => mockCreateGlobe(...args),
}));

jest.mock('motion/react', () => ({
  useMotionValue: (v: number) => ({ get: () => v, set: jest.fn() }),
  useSpring: (v: { get: () => number }) => ({ get: () => v.get(), set: jest.fn() }),
}));

describe('Globe', () => {
  beforeEach(() => {
    mockCreateGlobe.mockReturnValue({ destroy: mockDestroy });
    mockDestroy.mockClear();
  });

  it('renders a canvas and initializes the globe', () => {
    const { container } = render(<Globe />);
    expect(container.querySelector('canvas.globe-canvas')).toBeInTheDocument();
    expect(mockCreateGlobe).toHaveBeenCalledTimes(1);
  });

  it('destroys the globe on unmount', () => {
    const { unmount } = render(<Globe />);
    unmount();
    expect(mockDestroy).toHaveBeenCalledTimes(1);
  });
});
