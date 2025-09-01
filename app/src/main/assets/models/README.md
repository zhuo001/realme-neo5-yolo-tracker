# YOLOv5n NCNN Model Configuration
# This is a placeholder for YOLOv5 nano model
# Input: 640x640 RGB
# Output: 25200 detections (3 scales: 80x80, 40x40, 20x20)
# Classes: 80 (COCO dataset)

# Model files needed:
# yolov5n.param - Network structure
# yolov5n.bin   - Model weights

# To get real YOLOv5 NCNN models:
# 1. Download from: https://github.com/ultralytics/yolov5/releases
# 2. Convert to NCNN format using: https://convertmodel.com/
# 3. Or use: python export.py --weights yolov5n.pt --include ncnn

# For now, we'll use a minimal configuration that works with our detector stub
