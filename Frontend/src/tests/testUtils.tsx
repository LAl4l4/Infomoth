import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';

import { loginSlice } from '../Variable/login';
import { pageSlice } from '../Variable/pagenum';
import { profileSlice } from '../Variable/profile';
import { dataCacheSlice_ } from '../Variable/dataCache';

const rootReducer = combineReducers({
  login: loginSlice.reducer,
  page: pageSlice.reducer,
  profile: profileSlice.reducer,
  dataCache: dataCacheSlice_.reducer,
});

export type TestStore = ReturnType<typeof createTestStore>;

export function createTestStore(preloadedState?: Record<string, unknown>) {
  return configureStore({
    reducer: rootReducer,
    preloadedState: preloadedState as never,
  });
}

interface RenderOptions {
  store?: TestStore;
  route?: string;
}

/** Render a component wrapped in a fresh Redux Provider + MemoryRouter. */
export function renderWithProviders(
  ui: React.ReactElement,
  { store = createTestStore(), route = '/' }: RenderOptions = {}
) {
  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <Provider store={store}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </Provider>
    );
  }
  return { store, ...render(ui, { wrapper: Wrapper }) };
}
