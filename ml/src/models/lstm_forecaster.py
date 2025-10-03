"""
LSTM Forecasting Model for Blood Inventory Prediction
Deep Learning approach using Long Short-Term Memory networks
"""
import pandas as pd
import numpy as np
from typing import List, Dict, Tuple, Optional
import warnings
warnings.filterwarnings('ignore')

try:
    from sklearn.preprocessing import MinMaxScaler
    from sklearn.model_selection import train_test_split
    import tensorflow as tf
    from tensorflow import keras
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import LSTM, Dense, Dropout
    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False
    print("Warning: TensorFlow not installed. Install with: pip install tensorflow")


class LSTMForecaster:
    """
    LSTM-based forecasting model for blood inventory levels
    Uses deep learning for complex pattern recognition
    """

    def __init__(self, lookback: int = 7, lstm_units: int = 50, epochs: int = 50):
        """
        Initialize LSTM forecaster

        Args:
            lookback: Number of previous time steps to use for prediction
            lstm_units: Number of LSTM units in the hidden layer
            epochs: Number of training epochs
        """
        if not TENSORFLOW_AVAILABLE:
            raise ImportError("TensorFlow is not installed. Install with: pip install tensorflow")

        self.lookback = lookback
        self.lstm_units = lstm_units
        self.epochs = epochs
        self.model = None
        self.scaler = MinMaxScaler(feature_range=(0, 1))
        self.fitted = False

    def prepare_data(self, historical_data: List[Dict]) -> pd.Series:
        """
        Prepare historical data for LSTM model

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

    def create_sequences(self, data: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        """
        Create sequences for LSTM training

        Args:
            data: Scaled time series data

        Returns:
            Tuple of (X, y) sequences
        """
        X, y = [], []
        for i in range(len(data) - self.lookback):
            X.append(data[i:i + self.lookback])
            y.append(data[i + self.lookback])

        return np.array(X), np.array(y)

    def build_model(self) -> None:
        """Build LSTM model architecture"""
        self.model = Sequential([
            LSTM(self.lstm_units, activation='relu', return_sequences=True,
                 input_shape=(self.lookback, 1)),
            Dropout(0.2),
            LSTM(self.lstm_units // 2, activation='relu'),
            Dropout(0.2),
            Dense(1)
        ])

        self.model.compile(optimizer='adam', loss='mse', metrics=['mae'])

    def fit(self, time_series: pd.Series) -> None:
        """
        Fit LSTM model to time series data

        Args:
            time_series: pandas Series with datetime index
        """
        try:
            # Scale data
            values = time_series.values.reshape(-1, 1)
            scaled_data = self.scaler.fit_transform(values)

            # Create sequences
            X, y = self.create_sequences(scaled_data)

            if len(X) < 10:
                raise ValueError(f"Not enough data to create sequences (need at least {self.lookback + 10} points)")

            # Reshape for LSTM [samples, time steps, features]
            X = X.reshape(X.shape[0], X.shape[1], 1)

            # Build and train model
            self.build_model()

            # Train with reduced verbosity
            self.model.fit(
                X, y,
                epochs=self.epochs,
                batch_size=8,
                validation_split=0.1,
                verbose=0
            )

            self.fitted = True
            self.last_sequence = scaled_data[-self.lookback:]

        except Exception as e:
            raise ValueError(f"Error fitting LSTM model: {str(e)}")

    def predict(self, steps: int) -> Tuple[List[float], List[float], List[float]]:
        """
        Generate forecast

        Args:
            steps: Number of future periods to forecast

        Returns:
            Tuple of (predictions, lower_bounds, upper_bounds)
        """
        if not self.fitted:
            raise ValueError("Model must be fitted before prediction")

        predictions = []
        current_sequence = self.last_sequence.copy()

        # Generate predictions iteratively
        for _ in range(steps):
            # Prepare input
            X_pred = current_sequence.reshape(1, self.lookback, 1)

            # Predict next value
            pred_scaled = self.model.predict(X_pred, verbose=0)[0, 0]

            # Update sequence
            current_sequence = np.append(current_sequence[1:], pred_scaled)
            predictions.append(pred_scaled)

        # Inverse transform predictions
        predictions = np.array(predictions).reshape(-1, 1)
        predictions = self.scaler.inverse_transform(predictions).flatten()

        # Estimate confidence intervals (simplified - using prediction variance)
        std_dev = np.std(predictions) * 0.2  # 20% uncertainty
        lower_bounds = predictions - 1.96 * std_dev
        upper_bounds = predictions + 1.96 * std_dev

        return predictions.tolist(), lower_bounds.tolist(), upper_bounds.tolist()

    def forecast(self, historical_data: List[Dict], horizon_days: int) -> Dict:
        """
        Main forecasting method

        Args:
            historical_data: List of historical data points
            horizon_days: Number of days to forecast

        Returns:
            Dictionary with forecast results
        """
        # Prepare time series data
        time_series = self.prepare_data(historical_data)

        min_required = self.lookback + 20
        if len(time_series) < min_required:
            raise ValueError(f"Insufficient data for LSTM forecasting (minimum {min_required} data points required)")

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
                'predictedQuantity': round(float(predictions[i]), 2),
                'confidenceLower': round(float(lower_bounds[i]), 2),
                'confidenceUpper': round(float(upper_bounds[i]), 2),
                'confidenceLevel': 0.95
            })

        return {
            'success': True,
            'predictions': results,
            'modelInfo': {
                'type': 'LSTM',
                'lookback': self.lookback,
                'lstm_units': self.lstm_units,
                'epochs': self.epochs,
                'dataPoints': len(time_series)
            }
        }


def lstm_forecast(historical_data: List[Dict], horizon_days: int) -> Dict:
    """
    LSTM forecasting wrapper

    Args:
        historical_data: List of historical data points
        horizon_days: Number of days to forecast

    Returns:
        Dictionary with forecast results
    """
    if not TENSORFLOW_AVAILABLE:
        return {
            'success': False,
            'errorMessage': 'TensorFlow is not installed. Install with: pip install tensorflow'
        }

    forecaster = LSTMForecaster(lookback=7, lstm_units=50, epochs=50)
    return forecaster.forecast(historical_data, horizon_days)
