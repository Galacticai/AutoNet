package com.galacticai.autonet.common

fun tether(): String {
    return shellRunAsRoot("svc usb setFunctions rndis")
}

fun isTethered(): Boolean {
    val usbFunctions = shellRunAsRoot("svc usb getFunctions")
    return usbFunctions.contains("rndis")
}