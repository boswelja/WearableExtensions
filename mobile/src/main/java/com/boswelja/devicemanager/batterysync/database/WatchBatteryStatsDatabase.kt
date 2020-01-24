/* Copyright (C) 2019 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.batterysync.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchBatteryStats::class], version = 1)
abstract class WatchBatteryStatsDatabase : RoomDatabase() {

    abstract fun batteryStatsDao(): BatteryStatsDao
}
