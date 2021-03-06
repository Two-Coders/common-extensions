package com.twocoders.extensions.lifecycle.livedata.preference

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EncryptedSharedPreferencesLiveDataTest {

    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {

        val context = InstrumentationRegistry.getInstrumentation().context

        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun testLoadValueFromSharedPreferences() {
        val key = "testGetKey"

        val liveData = sharedPreferences.liveData(key, 34)
        assertEquals(liveData.value, 34)

        val testObserver = runBlocking(Dispatchers.Main) {
            liveData.test().also {
                sharedPreferences[key] = 56
            }
        }

        testObserver.awaitNextValue(200L, TimeUnit.MILLISECONDS).assertValueHistory(34, 56)
    }

    @Test
    fun testPutValueToSharedPreferences() {
        runBlocking(Dispatchers.Main) {
            val key = "testPutKey"

            val liveData = sharedPreferences.liveData(key, 34)
            liveData.test().assertValue(34)

            liveData.value = 89

            val storedValue: Int = sharedPreferences[key]
            assertEquals(storedValue, 89)
        }
    }

    @Test
    fun testPostValueToSharedPreferences() {
        val key = "testPostKey"

        val liveData = sharedPreferences.liveData(key, 0)
        liveData.postValue(64)

        val testObserver = runBlocking(Dispatchers.Main) { liveData.test() }

        testObserver.awaitNextValue(200L, TimeUnit.MILLISECONDS).assertValue(64)
    }
}