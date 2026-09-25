package org.opensearch.commons.alerting.action

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.common.io.stream.BytesStreamOutput
import org.opensearch.commons.alerting.builder
import org.opensearch.commons.alerting.model.Alert
import org.opensearch.commons.alerting.randomUser
import org.opensearch.commons.alerting.util.string
import org.opensearch.core.common.io.stream.StreamInput
import org.opensearch.core.xcontent.ToXContent
import java.time.Instant
import java.util.Collections

class GetAlertsResponseTests {

    @Test
    fun `test get alerts response with no alerts`() {
        val req = GetAlertsResponse(Collections.emptyList(), 0)
        assertNotNull(req)

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetAlertsResponse(sin)
        Assertions.assertTrue(newReq.alerts.isEmpty())
        assertEquals(0, newReq.totalAlerts)
    }

    @Test
    fun `test get alerts response with alerts`() {
        val alert = Alert(
            monitorId = "id",
            monitorName = "name",
            monitorVersion = Alert.NO_VERSION,
            monitorUser = randomUser(),
            triggerId = "triggerId",
            triggerName = "triggerNamer",
            state = Alert.State.ACKNOWLEDGED,
            startTime = Instant.now(),
            lastNotificationTime = null,
            errorMessage = null,
            errorHistory = emptyList(),
            severity = "high",
            actionExecutionResults = emptyList(),
            schemaVersion = 0,
            aggregationResultBucket = null,
            findingIds = emptyList(),
            relatedDocIds = emptyList(),
            executionId = "executionId",
            workflowId = "workflowId",
            workflowName = "",
            associatedAlertIds = emptyList()
        )
        val req = GetAlertsResponse(listOf(alert), 1)
        assertNotNull(req)

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetAlertsResponse(sin)
        assertEquals(1, newReq.alerts.size)
        assertEquals(alert, newReq.alerts[0])
        assertEquals(1, newReq.totalAlerts)
        assertEquals(newReq.alerts[0].workflowId, "workflowId")
    }

    @Test
    fun `test toXContent for get alerts response`() {
        val now = Instant.now()
        val alert = Alert(
            monitorId = "id",
            monitorName = "name",
            monitorVersion = Alert.NO_VERSION,
            monitorUser = randomUser(),
            triggerId = "triggerId",
            triggerName = "triggerNamer",
            state = Alert.State.ACKNOWLEDGED,
            startTime = now,
            lastNotificationTime = null,
            errorMessage = null,
            errorHistory = emptyList(),
            severity = "high",
            actionExecutionResults = emptyList(),
            schemaVersion = 0,
            aggregationResultBucket = null,
            findingIds = emptyList(),
            relatedDocIds = emptyList(),
            executionId = "executionId",
            workflowId = "wid",
            workflowName = "",
            associatedAlertIds = emptyList()
        )

        val req = GetAlertsResponse(listOf(alert), 1)
        var actualXContentString = req.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val expectedXContentString = "{\"alerts\":[{\"id\":\"\",\"version\":-1,\"monitor_id\":\"id\",\"workflow_id\":\"wid\"," +
            "\"workflow_name\":\"\",\"associated_alert_ids\":[],\"schema_version\":0,\"monitor_version\":-1," +
            "\"monitor_name\":\"name\",\"execution_id\":\"executionId\",\"trigger_id\":\"triggerId\"," +
            "\"trigger_name\":\"triggerNamer\",\"finding_ids\":[],\"related_doc_ids\":[],\"state\":\"ACKNOWLEDGED\"," +
            "\"error_message\":null,\"alert_history\":[],\"severity\":\"high\",\"action_execution_results\":[]," +
            "\"start_time\":${now.toEpochMilli()},\"last_notification_time\":null,\"end_time\":null," +
            "\"acknowledged_time\":null}],\"totalAlerts\":1}"
        assertEquals(expectedXContentString, actualXContentString)
    }

    @Test
    fun `test get alerts response with visible backend roles`() {
        val alert = randomAlert()
        val req = GetAlertsResponse(listOf(alert), 1, mapOf(alert.id to listOf("role-1", "role-2")))

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = GetAlertsResponse(sin)
        assertEquals(mapOf(alert.id to listOf("role-1", "role-2")), newReq.visibleBackendRoles)
    }

    @Test
    fun `test toXContent writes only the visible backend roles of the monitor user`() {
        val alert = randomAlert()
        val monitorUser = alert.monitorUser!!
        val req = GetAlertsResponse(listOf(alert), 1, mapOf(alert.id to listOf(monitorUser.backendRoles[0])))

        val xContentString = req.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        assertTrue(xContentString.contains("\"monitor_user\":{\"backend_roles\":[\"${monitorUser.backendRoles[0]}\"]}"))
        // The rest of the user object, and the backend role the requester does not belong to, stay hidden.
        assertFalse(xContentString.contains(monitorUser.name))
        assertFalse(xContentString.contains(monitorUser.backendRoles[1]))
        assertFalse(xContentString.contains("custom_attribute_names"))
    }

    @Test
    fun `test toXContent omits the monitor user when no visible backend roles are resolved`() {
        val alert = randomAlert()
        val req = GetAlertsResponse(listOf(alert), 1)

        val xContentString = req.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        assertFalse(xContentString.contains("monitor_user"))
    }

    private fun randomAlert(): Alert {
        return Alert(
            id = "alert-id",
            monitorId = "id",
            monitorName = "name",
            monitorVersion = Alert.NO_VERSION,
            monitorUser = randomUser(),
            triggerId = "triggerId",
            triggerName = "triggerNamer",
            state = Alert.State.ACKNOWLEDGED,
            startTime = Instant.now(),
            lastNotificationTime = null,
            errorMessage = null,
            errorHistory = emptyList(),
            severity = "high",
            actionExecutionResults = emptyList(),
            schemaVersion = 0,
            aggregationResultBucket = null,
            findingIds = emptyList(),
            relatedDocIds = emptyList(),
            executionId = "executionId",
            workflowId = "workflowId",
            workflowName = "",
            associatedAlertIds = emptyList()
        )
    }
}
