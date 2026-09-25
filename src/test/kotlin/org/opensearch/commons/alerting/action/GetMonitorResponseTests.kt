/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.alerting.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.common.io.stream.BytesStreamOutput
import org.opensearch.commons.alerting.builder
import org.opensearch.commons.alerting.model.CronSchedule
import org.opensearch.commons.alerting.model.Monitor
import org.opensearch.commons.alerting.randomUser
import org.opensearch.commons.alerting.util.string
import org.opensearch.core.common.io.stream.StreamInput
import org.opensearch.core.xcontent.ToXContent
import java.time.Instant
import java.time.ZoneId

class GetMonitorResponseTests {

    @Test
    fun `test get monitor response`() {
        val req = GetMonitorResponse("1234", 1L, 2L, 0L, null, null)
        assertNotNull(req)

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetMonitorResponse(sin)
        assertEquals("1234", newReq.id)
        assertEquals(1L, newReq.version)
        assertNull(newReq.monitor)
    }

    @Test
    fun `test get monitor response with monitor`() {
        val monitor = randomMonitor()
        val req = GetMonitorResponse("1234", 1L, 2L, 0L, monitor, null)
        assertNotNull(req)

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetMonitorResponse(sin)
        assertEquals("1234", newReq.id)
        assertEquals(1L, newReq.version)
        assertNotNull(newReq.monitor)
        assertNull(newReq.visibleBackendRoles)
    }

    @Test
    fun `test get monitor response with visible backend roles`() {
        val monitor = randomMonitor()
        val req = GetMonitorResponse("1234", 1L, 2L, 0L, monitor, null, listOf("role-1", "role-2"))

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetMonitorResponse(sin)
        assertNotNull(newReq.monitor)
        assertEquals(listOf("role-1", "role-2"), newReq.visibleBackendRoles)
    }

    @Test
    fun `test toXContent writes only the visible backend roles of the monitor user`() {
        val monitor = randomMonitor()
        val user = monitor.user!!
        val req = GetMonitorResponse("1234", 1L, 2L, 0L, monitor, null, listOf(user.backendRoles[0]))

        val xContentString = req.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        assertTrue(xContentString.contains("\"user\":{\"backend_roles\":[\"${user.backendRoles[0]}\"]}"))
        // The rest of the user object, and the backend role the requester does not belong to, stay hidden.
        assertFalse(xContentString.contains(user.name))
        assertFalse(xContentString.contains(user.backendRoles[1]))
        assertFalse(xContentString.contains("custom_attribute_names"))
    }

    @Test
    fun `test toXContent omits the user when no visible backend roles are resolved`() {
        val req = GetMonitorResponse("1234", 1L, 2L, 0L, randomMonitor(), null)

        val xContentString = req.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        assertFalse(xContentString.contains("\"user\""))
    }

    private fun randomMonitor(): Monitor {
        val cronExpression = "31 * * * *" // Run at minute 31.
        val testInstance = Instant.ofEpochSecond(1538164858L)

        val cronSchedule = CronSchedule(cronExpression, ZoneId.of("Asia/Kolkata"), testInstance)
        return Monitor(
            id = "123",
            version = 0L,
            name = "test-monitor",
            enabled = true,
            schedule = cronSchedule,
            lastUpdateTime = Instant.now(),
            enabledTime = Instant.now(),
            monitorType = Monitor.MonitorType.QUERY_LEVEL_MONITOR.value,
            user = randomUser(),
            schemaVersion = 0,
            inputs = mutableListOf(),
            triggers = mutableListOf(),
            uiMetadata = mutableMapOf()
        )
    }
}
