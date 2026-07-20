package com.chatassist.domain.model

/**
 * ROM 厂商枚举（v0.12：模块 10 后台保活）。
 *
 * 决定 [com.chatassist.util.VendorGuide] 构造的自启动引导 Intent 走哪条路。
 *
 * 来源：Build.MANUFACTURER（最权威） + 系统属性（ro.miui.ui.version.name / ro.build.version.emui 等）兜底。
 *
 * 涵盖范围（按 v2 指南 §4.10 必测 ROM 矩阵）：
 * - [XIAOMI]：MIUI / HyperOS（小米/红米）
 * - [HUAWEI]：EMUI / HarmonyOS（华为/荣耀）
 * - [OPPO]：ColorOS（OPPO/一加）
 * - [VIVO]：OriginOS（vivo/iQOO）
 * - [SAMSUNG]：One UI（三星）
 * - [MEIZU]：Flyme（魅族）
 * - [OTHER]：原生 AOSP / 其他小众（自动降级到通用 Intent）
 */
enum class Vendor {
    XIAOMI,
    HUAWEI,
    OPPO,
    VIVO,
    SAMSUNG,
    MEIZU,
    OTHER,
}
