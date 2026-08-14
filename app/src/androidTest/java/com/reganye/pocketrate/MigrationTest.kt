package com.reganye.pocketrate

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.reganye.pocketrate.data.local.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies that all handwritten migrations produce exactly the schema Room
 * expects at the current version. Runs on a device/emulator.
 *
 * The project only started exporting schemas at version 4, so the version-1
 * database is created here with plain SQL (matching the v1 entities: no
 * payerId, no settlement columns, no conversion_estimated). The final schema
 * is validated against the exported 4.json, which Room ships as an
 * androidTest asset.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To4() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = context.getDatabasePath(testDb)
        dbFile.delete()
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL("CREATE TABLE IF NOT EXISTS `exchange_rates` (`currencyCode` TEXT NOT NULL, `rateAgainstUsd` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`currencyCode`))")
            execSQL("CREATE TABLE IF NOT EXISTS `historical_rates` (`currencyCode` TEXT NOT NULL, `date` TEXT NOT NULL, `rateAgainstUsd` REAL NOT NULL, PRIMARY KEY(`currencyCode`, `date`))")
            execSQL("CREATE TABLE IF NOT EXISTS `trips` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `homeCurrency` TEXT NOT NULL, `startDate` INTEGER, `endDate` INTEGER, `budget` REAL, PRIMARY KEY(`id`))")
            execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `amount` REAL NOT NULL, `currency` TEXT NOT NULL, `convertedAmount` REAL NOT NULL, `rateUsed` REAL NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            execSQL("CREATE TABLE IF NOT EXISTS `companions` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            execSQL("CREATE TABLE IF NOT EXISTS `expense_splits` (`expenseId` TEXT NOT NULL, `companionId` TEXT NOT NULL, `share` REAL NOT NULL, PRIMARY KEY(`expenseId`, `companionId`), FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_tripId` ON `expenses` (`tripId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_companions_tripId` ON `companions` (`tripId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_companionId` ON `expense_splits` (`companionId`)")

            // Seed one trip so the 2->3 settlement-currency backfill is exercised.
            execSQL(
                "INSERT INTO trips (id, name, homeCurrency, startDate, endDate, budget) " +
                    "VALUES ('t1', 'Tokyo', 'USD', 0, 0, NULL)"
            )
            version = 1
            close()
        }

        helper.runMigrationsAndValidate(
            testDb,
            4,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        )
    }
}
