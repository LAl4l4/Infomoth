import { createSlice } from "@reduxjs/toolkit";

// slice for screen open state (is opening animation playing)
export const isOpenSlice = createSlice({
  name: 'isOpen',
  initialState: { isOpen: false },
  reducers: {
    setIsOpen(state, action) {
      state.isOpen = Boolean(action.payload);
    },
    toggleIsOpen(state) {
      state.isOpen = !state.isOpen;
    }
  }
});

export const { setIsOpen, toggleIsOpen } = isOpenSlice.actions;

export const selectIsOpen = (state) => state.isOpen.isOpen;
