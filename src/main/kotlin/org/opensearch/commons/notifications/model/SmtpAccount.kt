/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.commons.notifications.model

import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.commons.notifications.NotificationConstants.FROM_ADDRESS_TAG
import org.opensearch.commons.notifications.NotificationConstants.HOST_TAG
import org.opensearch.commons.notifications.NotificationConstants.METHOD_TAG
import org.opensearch.commons.notifications.NotificationConstants.PORT_TAG
import org.opensearch.commons.utils.logger
import org.opensearch.commons.utils.validateEmail
import org.opensearch.core.common.Strings
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import java.io.IOException

/**
 * Data class representing SMTP account channel.
 */
data class SmtpAccount(
    val host: String,
    val port: Int,
    val method: MethodType,
    val fromAddress: String
) : BaseConfigData {

    init {
        require(!Strings.isNullOrEmpty(host)) { "host is null or empty" }
        require(port > 0) { "port should be positive value" }
        validateEmail(fromAddress)
    }

    companion object {
        private val log by logger(SmtpAccount::class.java)

        /**
         * reader to create instance of class from writable.
         */
        val reader = Writeable.Reader { SmtpAccount(it) }

        /**
         * Parser to parse xContent
         */
        val xParser = XParser { parse(it) }

        @JvmStatic
        @Throws(IOException::class)
        fun parse(parser: XContentParser): SmtpAccount {
            var host: String? = null
            var port: Int? = null
            var method: MethodType? = null
            var fromAddress: String? = null

            XContentParserUtils.ensureExpectedToken(
                XContentParser.Token.START_OBJECT,
                parser.currentToken(),
                parser
            )
            while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = parser.currentName()
                parser.nextToken()
                when (fieldName) {
                    HOST_TAG -> host = parser.text()
                    PORT_TAG -> port = parser.intValue()
                    METHOD_TAG -> method = MethodType.fromTagOrDefault(parser.text())
                    FROM_ADDRESS_TAG -> fromAddress = parser.text()
                    else -> {
                        parser.skipChildren()
                        log.info("Unexpected field: $fieldName, while parsing SmtpAccount")
                    }
                }
            }
            host ?: throw IllegalArgumentException("$HOST_TAG field absent")
            port ?: throw IllegalArgumentException("$PORT_TAG field absent")
            method ?: throw IllegalArgumentException("$METHOD_TAG field absent")
            fromAddress ?: throw IllegalArgumentException("$FROM_ADDRESS_TAG field absent")
            return SmtpAccount(
                host,
                port,
                method,
                fromAddress
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun toXContent(builder: XContentBuilder?, params: ToXContent.Params?): XContentBuilder {
        return builder!!.startObject()
            .field(HOST_TAG, host)
            .field(PORT_TAG, port)
            .field(METHOD_TAG, method.tag)
            .field(FROM_ADDRESS_TAG, fromAddress)
            .endObject()
    }

    /**
     * Constructor used in transport action communication.
     * @param input StreamInput stream to deserialize data from.
     */
    constructor(input: StreamInput) : this(
        host = input.readString(),
        port = input.readInt(),
        method = input.readEnum(MethodType::class.java),
        fromAddress = input.readString()
    )

    /**
     * {@inheritDoc}
     */
    override fun writeTo(out: StreamOutput) {
        out.writeString(host)
        out.writeInt(port)
        out.writeEnum(method)
        out.writeString(fromAddress)
    }
}
