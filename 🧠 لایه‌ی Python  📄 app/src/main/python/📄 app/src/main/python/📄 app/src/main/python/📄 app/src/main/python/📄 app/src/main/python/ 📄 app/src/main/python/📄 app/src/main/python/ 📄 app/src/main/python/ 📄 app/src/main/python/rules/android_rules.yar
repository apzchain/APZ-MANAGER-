```
/*
    APZ Manager – Android YARA Rules (v1.0.0)
    License: GPLv3
*/

rule Android_SMS_Interception {
    meta:
        author = "APZ"
        severity = "high"
        description = "کلاس‌های مرتبط با رهگیری پیامک"
    strings:
        $s1 = "android/telephony/SmsMessage" ascii
        $s2 = "android.provider.Telephony.SMS_RECEIVED" ascii
        $s3 = "android.permission.READ_SMS" ascii
        $s4 = "SmsManager" ascii
    condition:
        ($s1 or $s2) and $s3 and $s4
}

rule Android_Accessibility_Abuse {
    meta:
        author = "APZ"
        severity = "critical"
        description = "سوءاستفاده از AccessibilityService"
    strings:
        $s1 = "android.accessibilityservice.AccessibilityService" ascii
        $s2 = "BIND_ACCESSIBILITY_SERVICE" ascii
        $s3 = "performGlobalAction" ascii
        $s4 = "AccessibilityNodeInfo" ascii
    condition:
        2 of them
}

rule Android_Overlay_Attack {
    meta:
        author = "APZ"
        severity = "high"
        description = "حمله‌ی Overlay روی اپ‌های بانکی"
    strings:
        $s1 = "SYSTEM_ALERT_WINDOW" ascii
        $s2 = "TYPE_APPLICATION_OVERLAY" ascii
        $s3 = "WindowManager.LayoutParams" ascii
    condition:
        all of them
}

rule Android_Dropper {
    meta:
        author = "APZ"
        severity = "critical"
        description = "Dropper — دانلود و نصب payload"
    strings:
        $s1 = "DexClassLoader" ascii
        $s2 = "PathClassLoader" ascii
        $s3 = "REQUEST_INSTALL_PACKAGES" ascii
        $s4 = "application/vnd.android.package-archive" ascii
    condition:
        2 of them
}

rule Android_Reflection_Loader {
    meta:
        author = "APZ"
        severity = "high"
        description = "بارگذاری کد با Reflection"
    strings:
        $s1 = "java/lang/reflect/Method" ascii
        $s2 = "Class.forName" ascii
        $s3 = "getDeclaredMethod" ascii
    condition:
        2 of them
}

rule Android_Crypto_Miner {
    meta:
        author = "APZ"
        severity = "high"
        description = "استخراج غیرمجاز رمزارز"
    strings:
        $s1 = "stratum+tcp://" ascii
        $s2 = "monero" nocase ascii
        $s3 = "minergate" nocase ascii
        $s4 = "nicehash" nocase ascii
        $s5 = "cryptonight" nocase ascii
    condition:
        any of them
}

rule Android_Command_Exec {
    meta:
        author = "APZ"
        severity = "high"
        description = "اجرای دستورات shell"
    strings:
        $s1 = "Runtime.getRuntime" ascii
        $s2 = "ProcessBuilder" ascii
        $s3 = "/system/bin/sh" ascii
        $s4 = "exec(" ascii
    condition:
        ($s1 or $s2) and ($s3 or $s4)
}

rule Android_Credential_Harvest {
    meta:
        author = "APZ"
        severity = "critical"
        description = "سرقت اطلاعات ورود"
    strings:
        $s1 = "getText().toString()" ascii
        $s2 = "password" nocase ascii
        $s3 = "login" nocase ascii
        $s4 = "https://" ascii
    condition:
        $s1 and $s2 and $s3 and #s4 > 3
}

rule Android_Persistence_Receiver {
    meta:
        author = "APZ"
        severity = "medium"
        description = "Persistence با BroadcastReceiver"
    strings:
        $s1 = "BOOT_COMPLETED" ascii
        $s2 = "BroadcastReceiver" ascii
        $s3 = "RECEIVE_BOOT_COMPLETED" ascii
    condition:
        all of them
}

rule Android_Anti_Analysis {
    meta:
        author = "APZ"
        severity = "medium"
        description = "تلاش برای فرار از تحلیل"
    strings:
        $s1 = "isDebuggerConnected" ascii
        $s2 = "ro.debuggable" ascii
        $s3 = "/proc/self/maps" ascii
        $s4 = "XposedBridge" ascii
        $s5 = "frida" nocase ascii
    condition:
        2 of them
}
```
