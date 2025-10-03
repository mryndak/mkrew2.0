"""
Prophet Forecasting Model for Blood Inventory Prediction
"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
import warnings
warnings.filterwarnings('ignore')

try:
    from prophet import Prophet
    PROPHET_AVAILABLE = True
except ImportError:
    PROPHET_AVAILABLE = False
    print("Warning: Prophet library not installed. Install with: pip install prophet")


class ProphetForecaster:
    """
    Prophet-based forecasting model for blood inventory levels
    Supports seasonality and holiday effects
    """

    def __init__(self):
        """Initialize Prophet forecaster"""
        if not PROPHET_AVAILABLE:
            raise ImportError("Prophet library is not installed. Install with: pip install prophet")

        self.model = None
        self.fitted = False

    def prepare_data(self, historical_data: List[Dict]) -> pd.DataFrame:
        """
        Prepare historical data for Prophet model

        Args:
            historical_data: List of dictionaries with timestamp, status, quantityLevel

        Returns:
            pandas DataFrame with 'ds' (datetime) and 'y' (value) columns
        """
        # Convert to DataFrame
        df = pd.DataFrame(historical_data)

        # Parse timestamps
        df['ds'] = pd.to_datetime(df['timestamp'])

        # Convert status to numeric if quantityLevel is not available
        if 'quantityLevel' in df.columns and df['quantityLevel'].notna().any():
            df['y'] = df['quantityLevel']
        else:
            # Map status to numeric values
            status_map = {
                'CRITICALLY_LOW': 1,
                'LOW': 2,
                'MEDIUM': 3,
                'SATISFACTORY': 4,
                'OPTIMAL': 5
            }
            df['y'] = df['status'].map(status_map)

        # Select only required columns and sort
        df = df[['ds', 'y']].sort_values('ds')

        # Handle missing values with forward fill
        df['y'] = df['y'].fillna(method='ffill')

        return df

    def fit(self, df: pd.DataFrame) -> None:
        """
        Fit Prophet model to time series data

        Args:
            df: pandas DataFrame with 'ds' and 'y' columns
        """
        try:
            # Initialize Prophet with custom settings
            self.model = Prophet(
                yearly_seasonality=True,
                weekly_seasonality=True,
                daily_seasonality=False,
                seasonality_mode='multiplicative',
                interval_width=0.95
            )

            # Fit the model
            self.model.fit(df)
            self.fitted = True

        except Exception as e:
            raise ValueError(f"Error fitting Prophet model: {str(e)}")

    def predict(self, periods: int) -> pd.DataFrame:
        """
        Generate forecast

        Args:
            periods: Number of future periods to forecast

        Returns:
            pandas DataFrame with predictions
        """
        if not self.fitted:
            raise ValueError("Model must be fitted before prediction")

        # Create future dataframe
        future = self.model.make_future_dataframe(periods=periods, freq='D')

        # Get forecast
        forecast = self.model.predict(future)

        # Return only future predictions
        return forecast.tail(periods)

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
        df = self.prepare_data(historical_data)

        if len(df) < 14:
            raise ValueError("Insufficient data for forecasting (minimum 14 data points required for Prophet)")

        # Fit model
        self.fit(df)

        # Generate predictions
        forecast_df = self.predict(horizon_days)

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

        # Build result
        results = []
        for _, row in forecast_df.iterrows():
            results.append({
                'forecastDate': row['ds'].isoformat(),
                'predictedStatus': value_to_status(row['yhat']),
                'predictedQuantity': round(row['yhat'], 2),
                'confidenceLower': round(row['yhat_lower'], 2),
                'confidenceUpper': round(row['yhat_upper'], 2),
                'confidenceLevel': 0.95
            })

        return {
            'success': True,
            'predictions': results,
            'modelInfo': {
                'type': 'PROPHET',
                'seasonality': 'multiplicative',
                'dataPoints': len(df)
            }
        }


def prophet_forecast(historical_data: List[Dict], horizon_days: int) -> Dict:
    """
    Prophet forecasting wrapper

    Args:
        historical_data: List of historical data points
        horizon_days: Number of days to forecast

    Returns:
        Dictionary with forecast results
    """
    if not PROPHET_AVAILABLE:
        return {
            'success': False,
            'errorMessage': 'Prophet library is not installed. Install with: pip install prophet'
        }

    forecaster = ProphetForecaster()
    return forecaster.forecast(historical_data, horizon_days)
