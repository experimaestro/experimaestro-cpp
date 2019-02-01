// @flow

import "regenerator-runtime/runtime";
import { put, takeEvery, all, take } from 'redux-saga/effects'

import client from './client'

/// Retrieve the seasons
function* refreshExperimentsSaga() : any {
  client.send("Refresh! Aaaah!!!")     
}

export default function* rootSaga() : any {
    yield all([
      takeEvery("REFRESH_EXPERIMENTS", refreshExperimentsSaga)
    ])
  }
