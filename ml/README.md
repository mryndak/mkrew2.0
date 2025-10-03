# ML Forecasting Service

Machine Learning service for blood inventory forecasting using ARIMA models.

## Features

- **ARIMA Forecasting**: Time series prediction for blood inventory levels
- **REST API**: Flask-based API for integration with backend
- **Confidence Intervals**: 95% confidence intervals for predictions
- **Automatic Status Mapping**: Converts numeric predictions to status categories

## Requirements

- Python 3.10+
- pip

## Installation

```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

## Configuration

Copy `.env.example` to `.env` and adjust settings:

```bash
cp .env.example .env
```

## Running the Service

### Development Mode

```bash
python src/api/app.py
```

The service will start on `http://localhost:5000`

### Production Mode

```bash
gunicorn -w 4 -b 0.0.0.0:5000 src.api.app:app
```

## API Endpoints

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "service": "ML Forecasting Service",
  "version": "1.0.0"
}
```

### Create Forecast

```http
POST /api/forecast
Content-Type: application/json
```

**Request Body:**
```json
{
  "modelType": "ARIMA",
  "bloodType": "A_PLUS",
  "forecastHorizonDays": 7,
  "historicalData": [
    {
      "timestamp": "2024-01-01T12:00:00",
      "status": "MEDIUM",
      "quantityLevel": 3
    },
    {
      "timestamp": "2024-01-02T12:00:00",
      "status": "LOW",
      "quantityLevel": 2
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "predictions": [
    {
      "forecastDate": "2024-01-03T00:00:00",
      "predictedStatus": "MEDIUM",
      "predictedQuantity": 3.2,
      "confidenceLower": 2.5,
      "confidenceUpper": 3.9,
      "confidenceLevel": 0.95
    }
  ],
  "modelInfo": {
    "type": "ARIMA",
    "order": "(1, 1, 1)",
    "dataPoints": 30
  }
}
```

### List Available Models

```http
GET /api/models
```

**Response:**
```json
{
  "models": [
    {
      "type": "ARIMA",
      "name": "AutoRegressive Integrated Moving Average",
      "description": "Classical time series forecasting model",
      "parameters": {
        "p": 1,
        "d": 1,
        "q": 1
      },
      "status": "active"
    }
  ]
}
```

## Data Format

### Historical Data

Each data point should contain:

- `timestamp`: ISO 8601 datetime string
- `status`: One of `CRITICALLY_LOW`, `LOW`, `MEDIUM`, `SATISFACTORY`, `OPTIMAL`
- `quantityLevel`: (Optional) Numeric representation (1-5)

### Status Mapping

| Status | Numeric Value | Range |
|--------|--------------|-------|
| CRITICALLY_LOW | 1 | < 1.5 |
| LOW | 2 | 1.5 - 2.5 |
| MEDIUM | 3 | 2.5 - 3.5 |
| SATISFACTORY | 4 | 3.5 - 4.5 |
| OPTIMAL | 5 | > 4.5 |

## Model Details

### ARIMA(1,1,1)

- **p=1**: Autoregressive order (uses 1 previous value)
- **d=1**: Differencing degree (first-order differencing)
- **q=1**: Moving average order (uses 1 previous error)

The model automatically:
- Handles missing values with forward fill
- Generates confidence intervals
- Maps numeric predictions to status categories

## Testing

```bash
# Run tests (when available)
pytest tests/

# Test API with curl
curl http://localhost:5000/health
```

## Docker

Build and run with Docker:

```bash
# Build image
docker build -t ml-forecasting-service .

# Run container
docker run -p 5000:5000 ml-forecasting-service
```

## Integration with Backend

The backend service calls this ML service at:
- Default URL: `http://localhost:5000`
- Configurable via `ml.service.url` in backend's `application.properties`

## Future Enhancements

- [ ] Prophet model implementation
- [ ] SARIMA for seasonal patterns
- [ ] LSTM neural network models
- [ ] Automatic parameter optimization (auto ARIMA)
- [ ] Model persistence and caching
- [ ] Batch prediction endpoint
- [ ] Model performance metrics endpoint

## Troubleshooting

**Error: "Insufficient data for forecasting"**
- Ensure at least 10 historical data points are provided

**Error: "Error fitting ARIMA model"**
- Check data quality and remove outliers
- Ensure data has sufficient variance

**Import errors**
- Verify all dependencies are installed: `pip install -r requirements.txt`
- Check Python version: `python --version` (must be 3.10+)
