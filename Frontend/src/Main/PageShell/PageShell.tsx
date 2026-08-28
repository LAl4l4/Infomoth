import './PageShell.css';
import { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion, type Variants } from 'motion/react';
import { Navigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { selectPageNum, setPageNum } from '../../Variable/pagenum';
import { restoreSessionThunk, selectIsLoggedIn, selectSessionChecked } from '../../Variable/login';
import { pullDisplaySettings, pullGeneralSettings } from '../../API/settings';
import LoginIcon from '../LoginIcon/LoginIcon';
//import { BgGlobe } from '../BackgroundGlobe/BackgroundGlobe';
import Globe, { GLOBE_CONFIG } from '../BackgroundGlobe/Globe';
import TabBar from '../TabBar/TabBar';
import OverviewTab from '../Contents/OverviewTab';
import SentimentTab from '../Contents/SentimentTab';
import AISkillsTab from '../Contents/AISkillsTab';
import ExchangeRateTab from '../Contents/ExchangeRateTab';
import UsStockTab from '../Contents/UsStockTab';
import MarketTrendTab from '../Contents/MarketTrendTab';
import type { AppDispatch, TabItem } from '../../customTypes';
import { DEFAULT_DISPLAY_SETTINGS, hexToRgb } from '../../displaySettings';

const TABS: TabItem[] = [
  { key: 0, label: '概览' },
  { key: 1, label: '市场情绪' },
  { key: 2, label: 'AI技能' },
  { key: 3, label: '汇率' },
  { key: 4, label: '美股' },
  { key: 5, label: '市场走势' },
];

function clampTab(n: number): number {
  return Math.min(Math.max(n, 0), TABS.length - 1);
}

// Accumulated horizontal wheel distance (px) that makes up one swipe unit.
// 180px keeps a light flick from advancing several tabs at once.
const SWIPE_THRESHOLD = 180;
// Pause (ms) without wheel events that ends a swipe gesture. The next event
// after a gap starts a fresh gesture whose accumulated distance restarts at
// zero relative to the current tab.
const SWIPE_GAP_MS = 250;

// Map accumulated swipe units to a tab offset using resident intervals: the
// current tab owns (-0.5, 0.5], the first neighbour owns (0.5, 1.5], the
// second (1.5, 2.5], and so on (mirrored for the negative side). Re-entering
// the same interval keeps the tab, so wobbling inside an interval does not
// flip tabs back and forth. Exact half boundaries round toward zero.
function swipeOffset(units: number): number {
  return Math.sign(units) * Math.ceil(Math.abs(units) - 0.5);
}

// Horizontal slide distance (px) for the tab enter/exit transition.
const SLIDE_DISTANCE = 80;

const slideTransition = {
  type: 'tween',
  duration: 0.28,
  ease: 'easeInOut',
} as const;

export default function PageShell() {
  const pagenum = useSelector(selectPageNum);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const sessionChecked = useSelector(selectSessionChecked);
  const dispatch = useDispatch<AppDispatch>();
  const [displaySettings, setDisplaySettings] = useState(DEFAULT_DISPLAY_SETTINGS);

  const safeTab = clampTab(pagenum);

  // Direction of the latest tab change (1 = next, -1 = previous), used to
  // decide which way the content slides. Read during render; written in the
  // effect below after commit.
  const prevTabRef = useRef(safeTab);
  const direction = safeTab >= prevTabRef.current ? 1 : -1;

  useEffect(() => {
    prevTabRef.current = safeTab;
  }, [safeTab]);

  // Whether the tab change that is currently animating came from a trackpad
  // swipe. Click and arrow-key switches are instant, so only swipe changes get
  // the slide transition. This lives in a ref (not state) so the exit variant
  // of the outgoing tab reads the mode of the *current* change, not the mode
  // of whatever change mounted it.
  const swipeModeRef = useRef(false);

  const variants: Variants = {
    enter: (dir: number) =>
      swipeModeRef.current
        ? { x: dir > 0 ? SLIDE_DISTANCE : -SLIDE_DISTANCE, opacity: 0 }
        : { x: 0, opacity: 1, transition: { duration: 0 } },
    center: { x: 0, opacity: 1 },
    exit: (dir: number) =>
      swipeModeRef.current
        ? { x: dir > 0 ? -SLIDE_DISTANCE : SLIDE_DISTANCE, opacity: 0 }
        : { opacity: 0, transition: { duration: 0 } },
  };

  useEffect(() => {
    dispatch(restoreSessionThunk());
  }, [dispatch]);

  useEffect(() => {
    if (!isLoggedIn) return;

    let active = true;
    pullGeneralSettings()
      .then(({ defaultPage }) => {
        if (active) dispatch(setPageNum(defaultPage));
      })
      .catch(() => {
        // Keep the overview tab when settings cannot be loaded.
      });

    return () => {
      active = false;
    };
  }, [dispatch, isLoggedIn]);

  useEffect(() => {
    if (!isLoggedIn) return;

    let active = true;
    pullDisplaySettings()
      .then((settings) => {
        if (active) setDisplaySettings(settings);
      })
      .catch(() => {
        // Keep the current InfoMoth palette when display settings are unavailable.
      });

    return () => {
      active = false;
    };
  }, [isLoggedIn]);

  const globeConfig = useMemo(() => ({
    ...GLOBE_CONFIG,
    baseColor: hexToRgb(displaySettings.globePointColor),
    glowColor: hexToRgb(displaySettings.globeGlowColor),
    markerColor: hexToRgb(displaySettings.globeMarkerColor),
  }), [displaySettings]);

  // Switch tabs with the left / right arrow keys.
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;

      // Don't hijack arrow keys while the user is typing.
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable)
      ) {
        return;
      }

      e.preventDefault();
      swipeModeRef.current = false;
      dispatch(setPageNum(clampTab(safeTab + (e.key === 'ArrowRight' ? 1 : -1))));
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [dispatch, safeTab]);

  // Switch tabs with a horizontal two-finger trackpad swipe.
  //
  // The listener must be attached to a concrete element (not window/document),
  // because browsers force wheel listeners there to be passive and then ignore
  // preventDefault(). We preventDefault() on horizontal wheels so the browser
  // does NOT treat the swipe as a back/forward navigation gesture.
  //
  // A swipe gesture accumulates distance from zero; the accumulated units are
  // mapped to a tab offset through resident intervals (see swipeOffset), so
  // the tab only changes when the total distance crosses into the next
  // interval and stays there while wobbling inside it. A pause longer than
  // SWIPE_GAP_MS ends the gesture: the next swipe restarts at zero relative
  // to whatever tab is active then.
  // The accumulator lives in this effect's closure, which deliberately does NOT
  // depend on `safeTab` so a tab change doesn't reset it mid-swipe; `safeTabRef`
  // keeps the current index available without re-subscribing.
  const shellRef = useRef<HTMLDivElement>(null);
  const safeTabRef = useRef(safeTab);

  useEffect(() => {
    safeTabRef.current = safeTab;
  }, [safeTab]);

  useEffect(() => {
    // The shell div only exists after the session check renders it, so gate
    // on that state — otherwise this effect runs with a null ref and never
    // attaches. (The div has already been committed when this effect runs.)
    const shell = shellRef.current;
    if (!shell) return;

    let accum = 0;
    let baseTab = safeTabRef.current;
    let lastAt = 0;

    const onWheel = (e: WheelEvent) => {
      // Only consume predominantly horizontal swipes; let vertical wheel scroll.
      if (Math.abs(e.deltaX) <= Math.abs(e.deltaY)) return;

      // Consume the event so Chrome/Safari don't run back/forward navigation.
      e.preventDefault();

      const now = Date.now();
      // A long pause ends the gesture: restart accumulation from zero at the
      // currently active tab.
      if (!lastAt || now - lastAt > SWIPE_GAP_MS) {
        accum = 0;
        baseTab = safeTabRef.current;
      }
      lastAt = now;

      accum += e.deltaX;

      const next = clampTab(baseTab + swipeOffset(accum / SWIPE_THRESHOLD));
      if (next !== safeTabRef.current) {
        swipeModeRef.current = true;
        dispatch(setPageNum(next));
      }
    };

    shell.addEventListener('wheel', onWheel, { passive: false });
    return () => shell.removeEventListener('wheel', onWheel);
  }, [dispatch, sessionChecked, isLoggedIn]);

  if (!sessionChecked) return null;
  if (!isLoggedIn) return <Navigate to="/login" replace />;

  return (
    <div
      className="page-shell"
      ref={shellRef}
      style={{ backgroundColor: displaySettings.backgroundColor }}
    >
      <Globe config={globeConfig} />
      <TabBar
        tabs={TABS}
        active={safeTab}
        onSelect={(k) => {
          swipeModeRef.current = false;
          dispatch(setPageNum(k));
        }}
      />
      <LoginIcon />
      <div className="tab-content-shell">
        <AnimatePresence mode="popLayout" custom={direction} initial={false}>
          <motion.div
            key={safeTab}
            className="tab-content-inner"
            custom={direction}
            variants={variants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={slideTransition}
          >
            {safeTab === 0 && <OverviewTab />}
            {safeTab === 1 && <SentimentTab />}
            {safeTab === 2 && <AISkillsTab />}
            {safeTab === 3 && <ExchangeRateTab />}
            {safeTab === 4 && <UsStockTab />}
            {safeTab === 5 && <MarketTrendTab />}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  );
}
