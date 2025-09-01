cmake_minimum_required(VERSION 3.22.1)

# 创建 native 库
add_library(yolo_tracker_native STATIC
    yolo_tracker_impl.cpp
)

target_include_directories(yolo_tracker_native PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# 链接 log 库
find_library(log-lib log)
target_link_libraries(yolo_tracker_native ${log-lib})