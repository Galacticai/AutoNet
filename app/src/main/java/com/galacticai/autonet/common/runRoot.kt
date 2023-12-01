package com.galacticai.autonet.common

import java.io.BufferedReader
import java.io.InputStreamReader

fun shellRunAsRoot(cmd: String): String {
    return shellRun("su -c $cmd")
}

fun shellRun(cmd: String): String {
    val process = Runtime.getRuntime().exec(cmd)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var read: Int
    val buffer = CharArray(4096)
    val output = StringBuffer()
    while (reader.read(buffer).also { read = it } > 0) {
        output.append(buffer, 0, read)
    }
    reader.close()
    process.waitFor()
    return output.toString()
}
