rootProject.name = "IoriCore"

include(
    "common",
    "api",
    "velocity",
    "bungee",
    "bukkit",
    "paper",
    "nms-api",
    "nms-v1_8_R3",
    "nms-v1_20_R3",
    "nms-v1_21_R1",
    "nms-v26_R1",
)

project(":nms-api").projectDir = file("NMS_api")
project(":nms-v1_8_R3").projectDir = file("NMS_v1_8_R3")
project(":nms-v1_20_R3").projectDir = file("NMS_v1_20_R3")
project(":nms-v1_21_R1").projectDir = file("NMS_21_R1")
project(":nms-v26_R1").projectDir = file("NMS_26_R1")
project(":paper").projectDir = file("paper")
