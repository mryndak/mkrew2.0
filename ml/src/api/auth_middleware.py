"""
API Key authentication middleware for ML service
"""
import os
from functools import wraps
from flask import request, jsonify


API_KEY_HEADER = 'X-API-Key'


def get_api_key():
    """Get API key from environment variable"""
    return os.environ.get('ML_API_KEY', 'change-this-secure-api-key-in-production-mkrew-ml-2024')


def require_api_key(f):
    """
    Decorator to require API key for endpoint

    Usage:
        @app.route('/api/endpoint')
        @require_api_key
        def my_endpoint():
            return jsonify({'status': 'ok'})
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        api_key = get_api_key()
        request_api_key = request.headers.get(API_KEY_HEADER)

        if not request_api_key:
            return jsonify({
                'success': False,
                'errorMessage': 'Missing API key'
            }), 401

        if request_api_key != api_key:
            return jsonify({
                'success': False,
                'errorMessage': 'Invalid API key'
            }), 403

        return f(*args, **kwargs)

    return decorated_function
