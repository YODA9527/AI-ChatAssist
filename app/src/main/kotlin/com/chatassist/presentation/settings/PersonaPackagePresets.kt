package com.chatassist.presentation.settings

/**
 * 人设方案「绑定 App 包名」快捷预设（v0.18 补强）。
 *
 * 参照无障碍模式 [com.chatassist.domain.parser.ChatParser.PRESETS] 的国内主流 IM 名单
 * （微信/QQ/钉钉/飞书/企业微信），扩展到国内主流的社交与办公交流 App，供设置页一键勾选切换。
 */
data class AppPackagePreset(
    /** 显示名（中文优先，海外/国际 App 用原名）。 */
    val label: String,
    /** Android 包名。 */
    val pkg: String,
)

/**
 * 按使用场景分组的预设清单：社交通讯 + 办公协作。
 * 无障碍模式原 5 个 IM 全部包含在内，保证人设绑定与解析预设一致。
 */
val PERSONA_PACKAGE_PRESET_GROUPS: List<Pair<String, List<AppPackagePreset>>> = listOf(
    "社交通讯" to listOf(
        AppPackagePreset("微信", "com.tencent.mm"),
        AppPackagePreset("QQ", "com.tencent.mobileqq"),
        AppPackagePreset("微博", "com.sina.weibo"),
        AppPackagePreset("小红书", "com.xingin.xhs"),
        AppPackagePreset("陌陌", "com.immomo.momo"),
        AppPackagePreset("探探", "com.p1.mobile.putong"),
        AppPackagePreset("Soul", "com.taoumao.soul"),
        AppPackagePreset("抖音", "com.ss.android.ugc.aweme"),
        AppPackagePreset("快手", "com.smile.gifmaker"),
    ),
    "办公协作" to listOf(
        AppPackagePreset("企业微信", "com.tencent.wework"),
        AppPackagePreset("钉钉", "com.alibaba.dingtalk"),
        AppPackagePreset("飞书", "com.ss.android.lark"),
        AppPackagePreset("腾讯会议", "com.tencent.wemeet.app"),
        AppPackagePreset("QQ邮箱", "com.tencent.androidqqmail"),
        AppPackagePreset("网易邮箱大师", "com.netease.mail"),
    ),
)
