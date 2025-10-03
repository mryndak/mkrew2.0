"""
SARIMA Forecasting Model for Blood Inventory Prediction
Seasonal ARIMA for time series with seasonal patterns
"""
import pandas as pd
import numpy as np
from statsmodels.tsa.statespace.sarimax import SARIMAX
from typing import List, Dict, Tuple, Optional
import warnings
warnings.filterwarnings('ignore')


class SARIMAForecaster:
    """
    SARIMA-based forecasting model for blood inventory levels
    Handles seasonal patterns in the data
    """

    def __init__(self, order: Tuple[int, int, int] = (1, 1, 1),
                 seasonal_order: Tuple[int, int, int, int] = (1, 1, 1, 7)):
        """
        Initialize SARIMA forecaster

        Args:
            order: (p, d, q) order of the non-seasonal ARIMA component
            seasonal_order: (P, D, Q, s) seasonal component
                P: seasonal AR order
                D: seasonal differencing
                Q: seasonal MA order
                s: seasonal period (7 for weekly, 30 for monthly)
        """
        self.order = order
        self.seasonal_order = seasonal_order
        self.model = None
        self.fitted_model = None

    def prepare_data(self, historical_data: List[Dict]) -> pd.Series:
        """
        Prepare historical data for SARIMA model

        Args:
            historical_data: List of dictionaries with timestamp, status, quantityLevel

        Returns:
            pandas Series with datetime index and numeric values
        """
        # Convert to DataFrame
        df = pd.DataFrame(historical_data)

        # Parse timestamps
        df['timestamp'] = pd.to_datetime(df['timestamp'])

        # Convert status to numeric if quantityLevel is not available
        if 'quantityLevel' in df.columns and df['quantityLevel'].notna().any():
            df['value'] = df['quantityLevel']
        else:
            # Map status to numeric values
            status_map = {
                'CRITICALLY_LOW': 1,
                'LOW': 2,
                'MEDIUM': 3,
                'SATISFACTORY': 4,
                'OPTIMAL': 5
            }
            df['value'] = df['status'].map(status_map)

        # Set timestamp as index and sort
        df = df.set_index('timestamp').sort_index()

        # Handle missing values with forward fill
        df['value'] = df['value'].fillna(method='ffill')

        return df['value']

    def fit(self, time_series: pd.Series) -> None:
        """
        Fit SARIMA model to time series data

        Args:
            time_series: pandas Series with datetime index
        """
        try:
            self.model = SARIMAX(
                time_series,
                order=self.order,
                seasonal_order=self.seasonal_order,
                enforce_stationarity=False,
                enforce_invertibility=False
            )
            self.fitted_model = self.model.fit(disp=False)
        except Exception as e:
            raise ValueError(f"Error fitting SARIMA model: {str(e)}")

    def predict(self, steps: int, confidence_level: float = 0.95) -> Tuple[List[float], List[float], List[float]]:
        """
        Generate forecast with confidence intervals

        Args:
            steps: Number of future periods to forecast
            confidence_level: Confidence level for prediction intervals (default 0.95)

        Returns:
            Tuple of (predictions, lower_bounds, upper_bounds)
        """
        if self.fitted_model is None:
            raise ValueError("Model must be fitted before prediction")

        # Get forecast with prediction intervals
        forecast_result = self.fitted_model.get_forecast(steps=steps)
        forecast = forecast_result.predicted_mean
        forecast_ci = forecast_result.conf_int(alpha=1-confidence_level)

        predictions = forecast.tolist()
        lower_bounds = forecast_ci.iloc[:, 0].tolist()
        upper_bounds = forecast_ci.iloc[:, 1].tolist()

        return predictions, lower_bounds, upper_bounds

    def forecast(self, historical_data: List[Dict], horizon_days: int) -> Dict:
        """
        Main forecasting method - prepares data, fits model, and generates predictions

        Args:
            historical_data: List of historical data points
            horizon_days: Number of days to forecast

        Returns:
            Dictionary with forecast results
        """
        # Prepare time series data
        time_series = self.prepare_data(historical_data)

        # SARIMA needs more data due to seasonal component
        min_required = max(14, self.seasonal_order[3] * 2)
        if len(time_series) < min_required:
            raise ValueError(f"Insufficient data for forecasting (minimum {min_required} data points required for SARIMA)")

        # Fit model
        self.fit(time_series)

        # Generate predictions
        predictions, lower_bounds, upper_bounds = self.predict(horizon_days)

        # Map predictions to status categories
        def value_to_status(value: float) -> str:
            if value < 1.5:
                return 'CRITICALLY_LOW'
            elif value < 2.5:
                return 'LOW'
            elif value < 3.5:
                return 'MEDIUM'
            elif value < 4.5:
                return 'SATISFACTORY'
            else:
                return 'OPTIMAL'

        # Generate forecast dates
        last_date = time_series.index[-1]
        forecast_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=horizon_days, freq='D')

        # Build result
        results = []
        for i, date in enumerate(forecast_dates):
            results.append({
                'forecastDate': date.isoformat(),
                'predictedStatus': value_to_status(predictions[i]),
                'predictedQuantity': round(predictions[i], 2),
                'confidenceLower': round(lower_bounds[i], 2),
                'confidenceUpper': round(upper_bounds[i], 2),
                'confidenceLevel': 0.95
            })

        return {
            'success': True,
            'predictions': results,
            'modelInfo': {
                'type': 'SARIMA',
                'order': f'{self.order}',
                'seasonal_order': f'{self.seasonal_order}',
                'dataPoints': len(time_series)
            }
        }


def sarima_forecast(historical_data: List[Dict], horizon_days: int) -> Dict:
    """
    SARIMA forecasting with weekly seasonality

    Args:
        historical_data: List of historical data points
        horizon_days: Number of days to forecast

    Returns:
        Dictionary with forecast results
    """
    # Default: SARIMA(1,1,1)x(1,1,1,7) - weekly seasonality
    forecaster = SARIMAForecaster(
        order=(1, 1, 1),
        seasonal_order=(1, 1, 1, 7)  # 7-day weekly pattern
    )
    return forecaster.forecast(historical_data, horizon_days)
