cmake_minimum_required(VERSION 3.22.1)
project("yolo_tracker")

set(CMAKE_CXX_STANDARD 17)

# 查找预编译的 ncnn
find_library(log-lib log)

# 添加 ncnn 预编译库路径 (需要下载 ncnn-android 包)
set(NCNN_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn-android)
message(STATUS "Looking for NCNN at: ${NCNN_DIR}")

if(EXISTS ${NCNN_DIR})
    add_library(ncnn STATIC IMPORTED)
    set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION
        ${NCNN_DIR}/lib/android/${ANDROID_ABI}/libncnn.a)
    target_include_directories(ncnn INTERFACE ${NCNN_DIR}/include)
    message(STATUS "Using NCNN from: ${NCNN_DIR}")
    message(STATUS "NCNN library: ${NCNN_DIR}/lib/android/${ANDROID_ABI}/libncnn.a")
    message(STATUS "NCNN headers: ${NCNN_DIR}/include")
else()
    message(WARNING "NCNN not found at ${NCNN_DIR}, building stub version")
    message(STATUS "Available directories:")
    execute_process(COMMAND ls -la ${CMAKE_SOURCE_DIR}/../../../.. 
                   OUTPUT_VARIABLE LS_OUTPUT)
    message(STATUS "${LS_OUTPUT}")
endif()

# 主库
add_library(yolo_tracker SHARED
    ../../../../native/src/jni_interface.cpp
    ../../../../native/src/detector.cpp
    ../../../../native/src/bytetrack.cpp
    ../../../../native/src/usb_protocol.cpp
    ../../../../native/src/nms.cpp
)

target_include_directories(yolo_tracker PRIVATE
    ../../../../native/include
)

target_link_libraries(yolo_tracker
    android
    ${log-lib}
)

# 如果有 ncnn，链接它
if(TARGET ncnn)
    target_link_libraries(yolo_tracker ncnn)
    target_compile_definitions(yolo_tracker PRIVATE USE_NCNN=1)
endif()
