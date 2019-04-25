package com.carles.carleskotlin.poi.data

import android.content.SharedPreferences
import com.carles.carleskotlin.common.data.setCacheExpirationTime
import com.carles.carleskotlin.poi.model.Poi
import io.mockk.*
import io.realm.Realm
import org.junit.Before
import org.junit.Test

class PoiLocalDatasourceTest {

    val sharedPreferences: SharedPreferences = mockk()
    val realm: Realm = mockk()
    val datasource = PoiLocalDatasource(sharedPreferences)

    @Before
    fun setup() {
        mockkStatic("io.realm.Realm")
        every { Realm.getDefaultInstance() } returns realm
    }

    @Test
    fun getPoiList_shouldReturnEmpty() {
        datasource.getPoiList().test().assertNoValues().assertComplete()
    }

    @Test
    fun getPoiDetail_shouldReturnNoValuesIfCacheExpired() {
        val spy = spyk(datasource)
        every { spy.isExpired(any(), any()) } returns true
        spy.getPoiDetail("some_id").test().assertNoValues().assertComplete()
        verify { Realm.getDefaultInstance() wasNot Called }
    }

    @Test
    fun getPoiDetail_shouldReturnNoValuesIfNoData() {
        val spy = spyk(datasource)
        every { spy.isExpired(any(), any()) } returns false
        every { realm.where(PoiRealmObject::class.java).equalTo(any(), any<String>()).findFirst() } returns null
        every { realm.close() } just Runs

        spy.getPoiDetail("some_id").test().assertNoValues().assertComplete()
        verify { realm.where(PoiRealmObject::class.java); realm.close() }
    }

    @Test
    fun getPoiDetail_shouldReturnStoredValues() {
        val spy = spyk(datasource)
        every { spy.isExpired(any(), any()) } returns false
        every { realm.where(PoiRealmObject::class.java).equalTo(any(), any<String>()).findFirst() } returns PoiRealmObject("some_id")
        every { realm.close() } just Runs

        spy.getPoiDetail("some_id").test().assertValue(Poi("some_id")).assertComplete()
        verify { realm.where(PoiRealmObject::class.java); realm.close() }
    }

    @Test
    fun persist_shouldPersistToRealm() {
        val poi = Poi("some_id")
        every { realm.executeTransaction(any()) } just Runs
        every { realm.close() } just Runs
        mockkStatic("com.carles.carleskotlin.common.data.DataExtensionsKt")
        every { sharedPreferences.setCacheExpirationTime(any(), any(), any()) } just Runs

        datasource.persist(poi)

        verify {
            realm.executeTransaction(any())
            realm.close()
            sharedPreferences.setCacheExpirationTime(PoiRealmObject::class.java.name, "some_id", any())
        }
    }
}