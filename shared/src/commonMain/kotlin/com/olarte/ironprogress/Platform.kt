package com.olarte.ironprogress

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform