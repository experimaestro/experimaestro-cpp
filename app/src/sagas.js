// @flow

import "regenerator-runtime/runtime";
import { takeEvery, all } from 'redux-saga/effects'

import client from './client'

/// Retrieve the seasons
function* refreshExperimentsSaga(action) : any {
  if (action.payload) {
    // We are connected
    yield client.send({action: "refresh" });
  }
}

export default function* rootSaga() : any {
    yield all([
      takeEvery("CONNECTED", refreshExperimentsSaga)
    ])
  }
