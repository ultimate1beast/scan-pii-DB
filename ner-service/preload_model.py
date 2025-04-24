#!/usr/bin/env python
"""
Preload GLiNER model from Hugging Face Hub and save locally
"""
import os
import logging
from pathlib import Path
import shutil
import sys

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def download_gliner_model():
    """
    Download the GLiNER model from Hugging Face Hub and save locally
    """
    try:
        from gliner import GLiNER
        
        # Create models directory if it doesn't exist
        model_dir = Path("models/E3-JSI_gliner-multi-pii-domains-v1")
        model_dir.mkdir(parents=True, exist_ok=True)
        
        logger.info(f"Downloading GLiNER model 'E3-JSI/gliner-multi-pii-domains-v1' to {model_dir}")
        
        # Download and save the model locally
        model = GLiNER.from_pretrained("E3-JSI/gliner-multi-pii-domains-v1")
        model.save_pretrained(str(model_dir))
        
        logger.info(f"Model successfully saved to {model_dir}")
        return True
        
    except Exception as e:
        logger.error(f"Error downloading model: {str(e)}")
        return False

if __name__ == "__main__":
    success = download_gliner_model()
    sys.exit(0 if success else 1)