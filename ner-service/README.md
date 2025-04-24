# PrivSense NER Service

This service provides Named Entity Recognition (NER) capabilities for the PrivSense application, specializing in detecting Personally Identifiable Information (PII) in text.

## Overview

The NER service uses the GLiNER model from E3-JSI specifically fine-tuned for PII entity detection. It exposes a REST API that accepts text samples and returns detected entities with their types and confidence scores.

## Features

- Fast and efficient PII detection using GLiNER model
- Support for multiple languages
- RESTful API for easy integration
- Health check endpoint for monitoring
- Local model caching to avoid repeated downloads
- Docker support for easy deployment

## PII Types Detected

The service can detect a wide range of PII types including but not limited to:
- Person names
- Organizations
- Addresses
- Phone numbers
- Email addresses
- Social Security Numbers
- Credit card information
- Medical information
- And many more

## Setup and Installation

### Prerequisites

- Python 3.8+
- pip
- Virtual environment (recommended)

### Local Setup

1. Create and activate a virtual environment (recommended):
   ```
   python -m venv venv
   venv\Scripts\activate  # On Windows
   source venv/bin/activate  # On Linux/Mac
   ```

2. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

3. Download the model:
   ```
   python preload_model.py
   ```

4. Start the service:
   ```
   python main.py --model-path "models\E3-JSI_gliner-multi-pii-domains-v1" --use-windows-certs --threads 8
   ```

   Or use the provided batch file:
   ```
   start.bat
   ```

### Docker Setup

1. Build the Docker image:
   ```
   docker build -t privsense-ner:latest .
   ```

2. Run the container:
   ```
   docker run -p 5000:5000 privsense-ner:latest
   ```

## API Usage

### Health Check

```
GET /health
```

Response:
```json
{
  "status": "ok",
  "model_loaded": true,
  "model_path": "models/E3-JSI_gliner-multi-pii-domains-v1"
}
```

### Detect PII Entities

```
POST /detect-pii
```

Request body:
```json
{
  "samples": [
    "My name is John Smith and my email is john.smith@example.com",
    "Credit card: 4111-1111-1111-1111, expires on 12/25"
  ]
}
```

Response:
```json
{
  "results": [
    [
      {
        "text": "John Smith",
        "type": "PER",
        "score": 0.98
      },
      {
        "text": "john.smith@example.com",
        "type": "EMAIL",
        "score": 0.99
      }
    ],
    [
      {
        "text": "4111-1111-1111-1111",
        "type": "CREDIT_CARD",
        "score": 0.95
      },
      {
        "text": "12/25",
        "type": "CREDIT_CARD_EXPIRATION_DATE",
        "score": 0.89
      }
    ]
  ]
}
```

## Integration with Java NerClientStrategy

This service is designed to work seamlessly with the `NerClientStrategy` component in the PrivSense Java application. The service implements the expected API contract:

1. The `/detect-pii` endpoint accepts a list of text samples
2. It returns entities with text, type, and score properties
3. Entity types are mapped to match the expected format in the Java code
4. The endpoint includes proper error handling

## Troubleshooting

Common issues:

1. **Model download fails**: Check your internet connection and proxy settings. The model is about 1.2GB.
2. **Service starts but model isn't loading**: Check for sufficient disk space and memory.
3. **SSL certificate errors**: Use the `--use-windows-certs` flag on Windows systems.

## Performance Considerations

- The service uses a single worker to avoid loading multiple instances of the model in memory
- For production usage with high throughput, consider running multiple instances behind a load balancer
- GPU acceleration is recommended for faster inference if available