package com.legnix.lxnav.util

import android.app.ActivityManager
import android.content.Context

/**
 * 读取设备可用内存，用于灵动岛"剩余内存"展示。
 */
object MemInfo {

    /** 系统剩余内存（MB） */
    fun getAvailableMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024L * 1024L)
    }

    /** 本进程已用内存（MB），用于后续标签内存保护判断 */
    fun getProcessUsedMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0L
        val pids = intArrayOf(android.os.Process.myPid())
        val memInfos = am.getProcessMemoryInfo(pids)
        return if (memInfos.isNotEmpty()) {
            memInfos[0].totalPss / 1024L
        } else 0L
    }
}
