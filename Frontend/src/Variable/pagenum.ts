import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { RootState } from "../customTypes";

interface PageState {
  pagenum: number;
}

const initialState: PageState = { pagenum: 0 };

// slice for page number (flip counter)
export const pageSlice = createSlice({
  name: 'page',
  initialState,
  reducers: {
    setPageNum(state, action: PayloadAction<number>) {
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

export const selectPageNum = (state: RootState) => state.page.pagenum;
