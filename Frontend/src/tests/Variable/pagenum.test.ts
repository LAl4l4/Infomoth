import {
  pageSlice,
  setPageNum,
  nextPage,
  prevPage,
  resetPage,
  selectPageNum,
} from '../../Variable/pagenum';
import type { RootState } from '../../customTypes';

const reducer = pageSlice.reducer;

describe('pagenum slice', () => {
  it('returns initial state', () => {
    expect(reducer(undefined, { type: 'unknown' })).toEqual({ pagenum: 0 });
  });

  it('setPageNum sets the page number', () => {
    expect(reducer({ pagenum: 0 }, setPageNum(3)).pagenum).toBe(3);
  });

  it('setPageNum coerces invalid payload to 0', () => {
    expect(reducer({ pagenum: 2 }, setPageNum(NaN)).pagenum).toBe(0);
  });

  it('nextPage / prevPage increment and decrement', () => {
    let state = reducer({ pagenum: 1 }, nextPage());
    expect(state.pagenum).toBe(2);
    state = reducer(state, prevPage());
    expect(state.pagenum).toBe(1);
  });

  it('resetPage returns to 0', () => {
    expect(reducer({ pagenum: 4 }, resetPage()).pagenum).toBe(0);
  });

  it('selectPageNum reads from state', () => {
    const state = { page: { pagenum: 2 } } as RootState;
    expect(selectPageNum(state)).toBe(2);
  });
});
