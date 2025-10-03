"""
Flask REST API for Blood Inventory Forecasting
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
import sys
import os

# Add parent directory to path for imports
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models.arima_forecaster import auto_arima_forecast

app = Flask(__name__)
CORS(app)  # Enable CORS for backend communication


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'service': 'ML Forecasting Service',
        'version': '1.0.0'
    }), 200


@app.route('/api/forecast', methods=['POST'])
def create_forecast():
    """
    Create blood inventory forecast using ARIMA model

    Request body:
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
            ...
        ]
    }

    Response:
    {
        "success": true,
        "predictions": [
            {
                "forecastDate": "2024-01-08T00:00:00",
                "predictedStatus": "MEDIUM",
                "predictedQuantity": 3.2,
                "confidenceLower": 2.5,
                "confidenceUpper": 3.9,
                "confidenceLevel": 0.95
            },
            ...
        ]
    }
    """
    try:
        # Parse request
        data = request.get_json()

        if not data:
            return jsonify({
                'success': False,
                'errorMessage': 'No JSON data provided'
            }), 400

        # Validate required fields
        model_type = data.get('modelType', 'ARIMA')
        horizon_days = data.get('forecastHorizonDays')
        historical_data = data.get('historicalData')

        if not horizon_days:
            return jsonify({
                'success': False,
                'errorMessage': 'forecastHorizonDays is required'
            }), 400

        if not historical_data or len(historical_data) == 0:
            return jsonify({
                'success': False,
                'errorMessage': 'historicalData is required and must not be empty'
            }), 400

        # Validate model type
        if model_type != 'ARIMA':
            return jsonify({
                'success': False,
                'errorMessage': f'Unsupported model type: {model_type}. Currently only ARIMA is supported.'
            }), 400

        # Generate forecast
        result = auto_arima_forecast(historical_data, horizon_days)

        return jsonify(result), 200

    except ValueError as e:
        return jsonify({
            'success': False,
            'errorMessage': f'Validation error: {str(e)}'
        }), 400

    except Exception as e:
        return jsonify({
            'success': False,
            'errorMessage': f'Internal error: {str(e)}'
        }), 500


@app.route('/api/models', methods=['GET'])
def list_models():
    """List available forecasting models"""
    return jsonify({
        'models': [
            {
                'type': 'ARIMA',
                'name': 'AutoRegressive Integrated Moving Average',
                'description': 'Classical time series forecasting model',
                'parameters': {
                    'p': 1,
                    'd': 1,
                    'q': 1
                },
                'status': 'active'
            }
        ]
    }), 200


@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'success': False,
        'errorMessage': 'Endpoint not found'
    }), 404


@app.errorhandler(500)
def internal_error(error):
    return jsonify({
        'success': False,
        'errorMessage': 'Internal server error'
    }), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('DEBUG', 'False').lower() == 'true'

    print(f"Starting ML Forecasting Service on port {port}")
    print(f"Debug mode: {debug}")

    app.run(host='0.0.0.0', port=port, debug=debug)
