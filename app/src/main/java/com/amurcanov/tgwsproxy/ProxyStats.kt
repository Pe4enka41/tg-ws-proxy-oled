package com.amurcanov.tgwsproxy

/** Live values exposed by the native engine, updated every few seconds. */
data class ProxyStats(
    val upBytes: Long = 0L,
    val downBytes: Long = 0L,
    val activeSessions: Int = 0
)
