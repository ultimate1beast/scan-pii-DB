# PrivSense NER Service

The NER (Named Entity Recognition) service is a crucial component of the PrivSense system that provides accurate detection of personally identifiable information (PII) in text data. This microservice uses advanced NLP techniques through the GLiNER model to identify sensitive information across multiple domains and languages.

## Overview

The NER service is a FastAPI-based REST API that exposes endpoints for PII detection in text. It uses the GLiNER (Generic Labeler for In-line Named Entity Recognition) model from E3-JSI to identify a wide range of PII entities such as names, addresses, phone numbers, credit card numbers, and many other sensitive data types.

## Features

- **High-accuracy PII detection**: Leverages state-of-the-art transformer-based models to accurately identify over 50 types of PII entities
- **Optimized performance**: Implements batch processing, parallel execution, and caching mechanisms for high throughput
- **Containerized deployment**: Easy deployment through Docker
- **Robust error handling**: Comprehensive error handling and reporting
- **Health monitoring**: Dedicated endpoint for monitoring service health

## Supported PII Types

The service detects numerous PII entity types, including but not limited to:

- Person names
- Organization names
- Phone numbers (mobile and landline)
- Email addresses
- Physical addresses
- Credit card numbers
- Social security numbers
- Passport numbers
- Driver's license numbers
- Health insurance IDs
- Dates of birth
- IP addresses
- Bank account numbers
- National ID numbers
- Medical conditions
- And many more

## API Endpoints

### 1. PII Detection

```
POST /detect-pii
```

Detects PII entities in provided text samples.

**Request Body:**
```json
{
  "samples": ["Text sample 1", "Text sample 2", ...]
}
```

**Response:**
```json
{
  "results": [
    [
      {
        "text": "John Doe",
        "type": "PER",
        "score": 0.98
      },
      ...
    ],
    ...
  ]
}
```

### 2. Health Check

```
GET /detect-pii/health
```

Returns service health status.

**Response:**
```json
{
  "status": "ok",
  "model_loaded": true,
  "model_path": "models/E3-JSI_gliner-multi-pii-domains-v1",
  "worker_pid": 12345
}
```

## Setup and Installation

### Prerequisites

- Python 3.10+
- Docker (optional)

### Local Installation

1. Clone the repository and navigate to the `ner-service` directory

2. Install the required packages:
   ```bash
   pip install -r requirements.txt
   ```

3. Download the GLiNER model:
   ```bash
   python preload_model.py
   ```

4. Start the service:
   ```bash
   python main.py --model-path models/E3-JSI_gliner-multi-pii-domains-v1
   ```

   Or with additional parameters:
   ```bash
   python main.py --model-path models/E3-JSI_gliner-multi-pii-domains-v1 --host 0.0.0.0 --port 5000 --threads 8 --workers 2
   ```

### Docker Deployment

1. Build the Docker image:
   ```bash
   docker build -t privsense/ner-service .
   ```

2. Run the container:
   ```bash
   docker run -p 5000:5000 privsense/ner-service
   ```

## Configuration Options

The service accepts the following command-line arguments:

| Argument | Description | Default |
|----------|-------------|---------|
| `--model-path` | Path to local model directory | Required (unless `--model-id` is specified) |
| `--model-id` | Hugging Face model ID for downloading | Required (unless `--model-path` is specified) |
| `--host` | Host IP to bind the service | 0.0.0.0 |
| `--port` | Port number to bind the service | 5000 |
| `--threads` | Number of worker threads for parallel processing | 8 |
| `--workers` | Number of Uvicorn worker processes | 1 |
| `--use-windows-certs` | Use Windows certificate store for SSL verification | False |

## Performance Optimization

The service implements several performance optimizations:
- **Thread pool**: Parallel processing of text samples using a configurable thread pool
- **Caching**: In-memory LRU cache for repeated text analysis
- **Batch processing**: Efficient batch processing when supported by the model
- **Multiprocessing support**: Shared memory for coordination between worker processes

## Integration with PrivSense

This NER service integrates with the PrivSense Java ecosystem through RESTful API calls. The module is primarily used by the `privsense-pii-detector` component to analyze text data and identify sensitive information.

## Quickstart with pre-built model

For quick testing, you can use the included batch script:
```bash
start.bat
```

This will start the service using the pre-downloaded model on the default port (5000).

## Technical Details

### Model

The service uses the E3-JSI/gliner-multi-pii-domains-v1 model, which is a specialized version of GLiNER for multi-domain PII detection. The model is automatically downloaded during the Docker build process or can be manually downloaded using the `preload_model.py` script.

### Architecture

The service follows a microservice architecture pattern:
- **FastAPI framework**: Provides the RESTful API interface
- **Uvicorn server**: ASGI server for handling HTTP requests
- **GLiNER model**: Core NLP engine for entity recognition
- **Thread pool**: Manages concurrent processing of requests

## Troubleshooting

### Common Issues

1. **Model loading failures**  
   Check that you have sufficient disk space and memory. The GLiNER model requires approximately 500MB of disk space.

2. **Slow processing time**  
   Adjust the `--threads` and `--workers` parameters based on your hardware capabilities.

3. **SSL certificate errors**  
   On Windows systems, you might encounter SSL certificate errors when downloading the model. Use the `--use-windows-certs` flag to resolve this.

## License

[License information would go here]