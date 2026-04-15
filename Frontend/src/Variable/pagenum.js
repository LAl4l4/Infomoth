import { createSlice } from "@reduxjs/toolkit";


// slice for page number (flip counter)
export const pageSlice = createSlice({
  name: 'page',
  initialState: { pagenum: 0 },
  reducers: {
    setPageNum(state, action) {
      state.pagenum = Number(action.payload) || 0;
    },
    nextPage(state) {
      state.pagenum = state.pagenum + 1;
    },
    prevPage(state) {
      state.pagenum = state.pagenum - 1;
    },
    resetPage(state) {
      state.pagenum = 0;
    }
  }
});

export const {
  setPageNum, nextPage, prevPage, resetPage
} = pageSlice.actions;

export const selectPageNum = (state) => state.page.pagenum;