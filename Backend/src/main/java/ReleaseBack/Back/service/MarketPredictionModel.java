package ReleaseBack.Back.service;

import java.time.Instant;
import java.util.List;
import ReleaseBack.Back.DTO.MarketSignalDTO.Prediction;
import ReleaseBack.Back.entity.MarketObservation;

/** Replace this bean to integrate a trained model without changing the API. */
public interface MarketPredictionModel {
    /** Bump this key when model parameters or feature definitions change. */
    default String cacheVersion() { return getClass().getName(); }

    Prediction predict(List<MarketObservation> observations, Instant now);
}
