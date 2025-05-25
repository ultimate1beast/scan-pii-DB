#!/usr/bin/env python
"""
FastAPI service for PII detection using GLiNER model - Optimized Version
"""
import argparse
import logging
import os
import time
from typing import List, Dict, Any, Optional
import threading
import multiprocessing
from pathlib import Path
import concurrent.futures
from functools import lru_cache
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Global variables
gliner_model = None
model_lock = threading.Lock()
is_model_ready = False
model_loading_error = None

# Use multiprocessing shared memory to coordinate between workers
try:
    # Create a shared value for model ready status
    model_ready = multiprocessing.Value('b', False)
except:
    # Fall back to thread-local if multiprocessing is not available
    model_ready = threading.local()
    model_ready.value = False

# Focused on direct PII types only (not quasi-identifiers)
pii_entity_types = [
    "person", "organization", "phone number", "address", "passport number",
    "email", "credit card number", "social security number", 
    "health insurance id number", "date of birth", "mobile phone number",
    "bank account number", "medication", "cpf", "driver's license number",
    "tax identification number", "medical condition", "identity card number",
    "national id number", "ip address", "email address", "iban",
    "credit card expiration date", "username", "health insurance number",
    "registration number", "student id number", "insurance number",
    "flight number", "landline phone number", "blood type", "cvv",
    "reservation number", "digital signature", "social media handle",
    "license plate number", "cnpj", "postal code", "serial number",
    "vehicle registration number", "credit card brand", "fax number",
    "visa number", "insurance company", "identity document number",
    "transaction number", "national health insurance number", "cvc",
    "birth certificate number", "train ticket number", "passport expiration date"
]

# Thread pool for parallel processing
thread_pool_executor = None
MAX_WORKERS = 8  # Increased from 4 to 8

# In-memory cache for storing recent results
CACHE_SIZE = 1000
request_cache = {}

# Pydantic models for request and response
class NerRequest(BaseModel):
    samples: List[str] = Field(..., description="List of text samples to analyze")

class NerEntity(BaseModel):
    text: str = Field(..., description="The detected entity text")
    type: str = Field(..., description="The entity type (PER, LOC, ORG, etc.)")
    score: float = Field(..., description="Confidence score (0.0-1.0)")

class NerResponse(BaseModel):
    results: List[List[NerEntity]] = Field(..., 
                             description="List of entity lists, one per input sample")

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_path: Optional[str] = None
    worker_pid: int


def load_model_in_background(model_path=None, model_id=None):
    """Load the GLiNER model in a background thread"""
    global gliner_model, is_model_ready, thread_pool_executor, model_loading_error, model_ready
    
    try:
        from gliner import GLiNER
        
        # Acquire lock to prevent concurrent model loading within this process
        with model_lock:
            # Check if another worker has already loaded the model
            if hasattr(model_ready, 'value') and model_ready.value:
                logger.info(f"Model already loaded by another worker, skipping load in this worker")
                is_model_ready = True
                return
                
            logger.info(f"Loading GLiNER model: {'from path: ' + model_path if model_path else 'from HF: ' + model_id}")
            start_time = time.time()
            
            try:
                if model_path:
                    # Load model from local path
                    gliner_model = GLiNER.from_pretrained(model_path)
                else:
                    # Load model from Hugging Face Hub
                    gliner_model = GLiNER.from_pretrained(model_id)
                
                load_time = time.time() - start_time
                logger.info(f"Model loaded successfully in {load_time:.2f} seconds")
                
                # Initialize thread pool after model is loaded
                thread_pool_executor = concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS)
                logger.info(f"Initialized thread pool with {MAX_WORKERS} workers")
                
                is_model_ready = True
                
                # Set shared memory flag if we're using multiprocessing
                if hasattr(model_ready, 'value'):
                    with model_ready.get_lock():
                        model_ready.value = True
                else:
                    model_ready.value = True
                    
            except Exception as e:
                error_msg = f"Error loading model: {str(e)}"
                logger.error(error_msg)
                model_loading_error = error_msg
                is_model_ready = False
    except Exception as e:
        error_msg = f"Critical error in model loading: {str(e)}"
        logger.error(error_msg)
        model_loading_error = error_msg
        is_model_ready = False


# Lifespan context manager (replacing on_event)
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup event
    logger.info("Service starting up")
    
    # Load model before accepting requests
    if not is_model_ready:
        logger.info("Loading model during startup...")
        # Load model directly rather than in background
        load_model_in_background(args.model_path, args.model_id)
    
    # Yield control back to FastAPI
    yield
    
    # Shutdown event
    global thread_pool_executor
    if thread_pool_executor:
        logger.info("Shutting down thread pool executor")
        thread_pool_executor.shutdown(wait=False)


# Create FastAPI app with lifespan handler
app = FastAPI(
    title="PrivSense NER Service",
    description="API for detecting PII entities in text using GLiNER model",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add middleware to ensure model is loaded
@app.middleware("http")
async def ensure_model_loaded(request: Request, call_next):
    global is_model_ready, model_loading_error
    
    # Skip check for health endpoint
    if request.url.path == "/health":
        return await call_next(request)
    
    # Check if model is ready
    if not is_model_ready:
        error_msg = model_loading_error or "Model is not loaded yet. Please try again later."
        return {
            "status_code": 503,
            "detail": error_msg
        }
    
    # Continue processing the request
    return await call_next(request)


@app.get("/detect-pii/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    global gliner_model, is_model_ready
    
    if is_model_ready and gliner_model:
        model_path = getattr(gliner_model, "model_path", None)
        return HealthResponse(
            status="ok",
            model_loaded=True,
            model_path=str(model_path) if model_path else None,
            worker_pid=os.getpid()
        )
    elif not is_model_ready and model_loading_error:
        return HealthResponse(
            status="error",
            model_loaded=False,
            worker_pid=os.getpid()
        )
    else:
        return HealthResponse(
            status="initializing",
            model_loaded=False,
            worker_pid=os.getpid()
        )


@lru_cache(maxsize=CACHE_SIZE)
def cached_process_text(text, entity_types_tuple, threshold):
    """Cached version of text processing for faster repeated analysis"""
    global gliner_model
    
    if not text.strip():
        return []
    
    try:
        # Convert tuple back to list for the model
        entity_types = list(entity_types_tuple)
        
        # Predict entities using GLiNER model
        entities = gliner_model.predict_entities(
            text,
            entity_types,
            threshold=threshold
        )
        
        # Convert to NerEntity objects
        ner_entities = []
        for entity in entities:
            entity_type = map_to_standard_entity_type(entity["label"])
            ner_entities.append({
                "text": entity["text"],
                "type": entity_type,
                "score": entity["score"]
            })
        
        return ner_entities
    
    except Exception as e:
        logger.error(f"Error processing sample: {str(e)}")
        return []


def process_text_sample(text, entity_types, threshold):
    """Process a single text sample using the cache if available"""
    # Convert list to tuple for caching (since tuples are hashable)
    return cached_process_text(text, tuple(entity_types), threshold)


def process_batch(texts, entity_types, threshold=0.0):
    """Process a batch of texts at once if the model supports batch processing"""
    global gliner_model
    
    # Filter out empty texts
    valid_texts = [(i, text) for i, text in enumerate(texts) if text.strip()]
    if not valid_texts:
        return [[] for _ in texts]
    
    try:
        # Check if batch processing is supported by the model
        if hasattr(gliner_model, "predict_entities_batch"):
            # Extract just the texts for batch processing
            batch_texts = [text for _, text in valid_texts]
            
            # Batch prediction
            batch_entities = gliner_model.predict_entities_batch(
                batch_texts,
                entity_types,
                threshold=threshold
            )
            
            # Process results and map back to original indices
            results = [[] for _ in texts]
            for idx, (original_idx, _) in enumerate(valid_texts):
                entities = batch_entities[idx]
                ner_entities = []
                for entity in entities:
                    entity_type = map_to_standard_entity_type(entity["label"])
                    ner_entities.append({
                        "text": entity["text"],
                        "type": entity_type,
                        "score": entity["score"]
                    })
                results[original_idx] = ner_entities
            
            return results
        else:
            # Fall back to individual processing
            results = [[] for _ in texts]
            for original_idx, text in valid_texts:
                results[original_idx] = process_text_sample(text, entity_types, threshold)
            return results
    except Exception as e:
        logger.error(f"Error processing batch: {str(e)}")
        return [[] for _ in texts]


def calc_cache_key(samples):
    """Calculate a simple hash key for the request cache"""
    # This is a simple hash function - could be improved for production
    key = ""
    for s in samples[:10]:  # Use first 10 samples to keep key size reasonable
        key += str(hash(s))
    return key


@app.post("/detect-pii", response_model=NerResponse)
async def detect_entities(request: NerRequest):
    """
    Detect PII entities in the provided text samples
    """
    global gliner_model, is_model_ready, thread_pool_executor, request_cache
    
    if not is_model_ready or gliner_model is None:
        raise HTTPException(
            status_code=503,
            detail="Model is not loaded yet. Please try again later."
        )
    
    if not request.samples:
        raise HTTPException(
            status_code=400, 
            detail="No text samples provided"
        )
    
    try:
        start_time = time.time()
        batch_size = min(len(request.samples), 100)  # Limit batch size
        samples = request.samples[:batch_size]
        
        # Check cache first
        cache_key = calc_cache_key(samples)
        if cache_key in request_cache:
            logger.info(f"Cache hit for request - returning cached result")
            return NerResponse(results=request_cache[cache_key])
            
        logger.info(f"Processing batch of {batch_size} samples")
        
        # Attempt batch processing if available, otherwise use thread pool
        if hasattr(gliner_model, "predict_entities_batch"):
            # Use batch processing
            results = process_batch(samples, pii_entity_types)
        else:
            # Use thread pool for parallel processing
            futures = []
            for text in samples:
                futures.append(
                    thread_pool_executor.submit(
                        process_text_sample, 
                        text, 
                        pii_entity_types, 
                        0.0  # Threshold changed to 0.0
                    )
                )
            
            # Collect results
            results = []
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())
                
            # Reorder results to match original sample order if needed
            ordered_results = [None] * len(futures)
            for i, future in enumerate(futures):
                ordered_results[i] = future.result()
            
            results = ordered_results
        
        # Cache the results
        if len(request_cache) >= CACHE_SIZE:
            # Simple cache eviction - clear if full
            request_cache.clear()
        request_cache[cache_key] = results
            
        elapsed = time.time() - start_time
        logger.info(f"Processed {batch_size} samples in {elapsed:.2f} seconds")
        
        return NerResponse(results=results)
    
    except Exception as e:
        error_msg = f"Error processing request: {str(e)}"
        logger.error(error_msg)
        raise HTTPException(
            status_code=500,
            detail=error_msg
        )


def map_to_standard_entity_type(entity_type: str) -> str:
    """Map GLiNER entity types to standard entity types expected by NerClientStrategy"""
    # Normalize entity type by uppercasing and removing spaces
    normalized = entity_type.upper().replace(" ", "_")
    
    # Map common GLiNER types to types expected by Java client
    mapping = {
        "PERSON": "PER",
        "PERSON_NAME": "PER",
        "ORGANIZATION": "ORG", 
        "PHONE_NUMBER": "PHONE",
        "MOBILE_PHONE_NUMBER": "PHONE",
        "LANDLINE_PHONE_NUMBER": "PHONE",
        "EMAIL": "EMAIL",
        "EMAIL_ADDRESS": "EMAIL",
        "SOCIAL_SECURITY_NUMBER": "SSN",
        "CREDIT_CARD_NUMBER": "CREDIT_CARD",
        "PASSPORT_NUMBER": "PASSPORT",
        "DRIVERS_LICENSE_NUMBER": "DRIVERS_LICENSE",
        "BANK_ACCOUNT_NUMBER": "BANK_ACCOUNT",
        "NATIONAL_ID_NUMBER": "NATIONAL_ID",
        "IDENTITY_CARD_NUMBER": "ID_CARD"
    }
    
    # Return mapped type or the normalized type if no mapping exists
    return mapping.get(normalized, normalized)


# Parse arguments at the module level for lifespan usage
parser = argparse.ArgumentParser(description="Start the NER service")
model_group = parser.add_mutually_exclusive_group(required=True)
model_group.add_argument("--model-path", help="Path to local model directory")
model_group.add_argument("--model-id", help="Hugging Face model ID")
parser.add_argument("--host", default="0.0.0.0", help="Host to bind the service to")
parser.add_argument("--port", type=int, default=5000, help="Port to bind the service to")
parser.add_argument("--threads", type=int, default=8, help="Number of worker threads")
parser.add_argument("--use-windows-certs", action="store_true", 
                  help="Use Windows certificate store for SSL verification")
parser.add_argument("--workers", type=int, default=1, 
                   help="Number of uvicorn workers")

args, _ = parser.parse_known_args()

# Set thread pool size based on arguments
MAX_WORKERS = args.threads

# Handle Windows certificate store if needed
if args.use_windows_certs:
    os.environ["HF_HUB_DISABLE_SSL_VERIFICATION"] = "1"
    os.environ["PYTHONHTTPSVERIFY"] = "0"
    

if __name__ == "__main__":
    # Re-parse the arguments to catch any modifications
    args = parser.parse_args()
    
    # Handle Windows certificate store if needed (again, to be sure)
    if args.use_windows_certs:
        os.environ["HF_HUB_DISABLE_SSL_VERIFICATION"] = "1"
        os.environ["PYTHONHTTPSVERIFY"] = "0"
        
    # Start the FastAPI server
    uvicorn.run(
        "main:app",  # Use module:app format for workers
        host=args.host,
        port=args.port,
        log_level="info",
        workers=args.workers,
    )