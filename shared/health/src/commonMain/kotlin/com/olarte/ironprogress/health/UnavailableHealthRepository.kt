package com.olarte.ironprogress.health

import com.olarte.ironprogress.domain.health.HealthAvailability
import com.olarte.ironprogress.domain.health.HealthRepository

class UnavailableHealthRepository : HealthRepository {
    override suspend fun availability(): HealthAvailability = HealthAvailability.NOT_SUPPORTED
}
