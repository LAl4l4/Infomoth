import { configureStore, combineReducers } from "@reduxjs/toolkit";

import { loginSlice } from "./login";
import { pageSlice } from "./pagenum";
import { profileSlice } from "./profile";
import { dataCacheSlice_ } from "./dataCache";


const rootReducer = combineReducers({
  login: loginSlice.reducer,
  page: pageSlice.reducer,
  profile: profileSlice.reducer,
  dataCache: dataCacheSlice_.reducer,
});

const store = configureStore({ reducer: rootReducer });


export default store;
