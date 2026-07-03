import { configureStore, combineReducers } from "@reduxjs/toolkit";

import { loginSlice } from "./login.js";
import { pageSlice } from "./pagenum.js";
import { profileSlice } from "./profile.js";
import { dataCacheSlice_ } from "./dataCache.js";


const rootReducer = combineReducers({
  login: loginSlice.reducer,
  page: pageSlice.reducer,
  profile: profileSlice.reducer,
  dataCache: dataCacheSlice_.reducer,
});

const store = configureStore({ reducer: rootReducer });


export default store;
