cmake_minimum_required(VERSION 3.22.1)
project("yolo_tracker")

set(CMAKE_CXX_STANDARD 17)

# 查找预编译的 ncnn
find_library(log-lib log)

# 添加 ncnn 预编译库路径 (需要下载 ncnn-android 包)
set(NCNN_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn-android)
if(EXISTS ${NCNN_DIR})
    add_library(ncnn STATIC IMPORTED)
    set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION
        ${NCNN_DIR}/lib/android/${ANDROID_ABI}/libncnn.a)
    target_include_directories(ncnn INTERFACE ${NCNN_DIR}/include)
    message(STATUS "Using NCNN from: ${NCNN_DIR}")
else()
    message(WARNING "NCNN not found at ${NCNN_DIR}, building stub version")
endif()

# 包含我们的 native 源码
add_subdirectory(../../../../native ${CMAKE_BINARY_DIR}/native_build)

# 主库
add_library(yolo_tracker SHARED
    tracker_jni.cpp
)

target_link_libraries(yolo_tracker
    android
    ${log-lib}
    yolo_tracker_native
)

# 如果有 ncnn，链接它
if(TARGET ncnn)
    target_link_libraries(yolo_tracker ncnn)
    target_compile_definitions(yolo_tracker PRIVATE USE_NCNN=1)
endif()
