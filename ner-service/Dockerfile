FROM python:3.10-slim

WORKDIR /app

# Copy requirements first to leverage Docker cache
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY main.py preload_model.py ./

# Create models directory
RUN mkdir -p models

# Download model at build time
RUN python preload_model.py

# Expose the port the app runs on
EXPOSE 5000

# Command to run the application
CMD ["python", "main.py", "--model-path", "models/E3-JSI_gliner-multi-pii-domains-v1", "--host", "0.0.0.0", "--port", "5000"]