cmake_minimum_required(VERSION 3.10)
project(yolo_tracker_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 如果集成 ncnn，请在外部提供 ncnn_DIR 或手动 add_library
# find_package(ncnn REQUIRED)

add_library(yolo_tracker_native STATIC
    src/detector.cpp
    src/bytetrack.cpp
    src/usb_protocol.cpp
    src/nms.cpp
    src/jni_interface.cpp
)

target_include_directories(yolo_tracker_native PUBLIC include)

# 如果找到 ncnn 则启用真实推理
find_package(ncnn)
if(ncnn_FOUND)
    message(STATUS "NCNN found, enabling real inference.")
    target_link_libraries(yolo_tracker_native PRIVATE ncnn)
    target_compile_definitions(yolo_tracker_native PRIVATE USE_NCNN=1)
else()
    message(WARNING "NCNN not found, tracker will be a stub.")
endif()
# target_link_libraries(yolo_tracker PRIVATE ncnn)
