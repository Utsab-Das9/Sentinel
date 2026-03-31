package com.privacynudge.vpn

import java.nio.ByteBuffer

/**
 * Simple DNS packet parser for extracting queried domain names.
 * Handles basic DNS query packets (UDP port 53).
 */
object DnsPacketParser {

    /**
     * Extract the queried domain name from a DNS query packet.
     * Returns null if the packet is not a valid DNS query or cannot be parsed.
     */
    fun extractQueryDomain(packet: ByteArray): String? {
        if (packet.size < 12) return null // DNS header is 12 bytes minimum

        val buffer = ByteBuffer.wrap(packet)

        // DNS Header (12 bytes)
        // Transaction ID (2 bytes)
        buffer.short

        // Flags (2 bytes)
        val flags = buffer.short.toInt() and 0xFFFF

        // Check if this is a query (QR bit = 0) - bit 15
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        // Question count (2 bytes)
        val questionCount = buffer.short.toInt() and 0xFFFF
        if (questionCount < 1) return null

        // Skip answer, authority, additional counts (6 bytes)
        buffer.short // Answer count
        buffer.short // Authority count
        buffer.short // Additional count

        // Parse the question section
        return try {
            parseDomainName(buffer)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse a domain name from DNS packet format.
     * DNS uses length-prefixed labels, e.g., [3]www[6]google[3]com[0]
     */
    private fun parseDomainName(buffer: ByteBuffer): String {
        val parts = mutableListOf<String>()
        var iterations = 0
        val maxIterations = 128 // Prevent infinite loops

        while (buffer.hasRemaining() && iterations < maxIterations) {
            val length = buffer.get().toInt() and 0xFF

            if (length == 0) {
                // End of domain name
                break
            }

            // Check for compression pointer (starts with 0xC0)
            if ((length and 0xC0) == 0xC0) {
                // Compression pointer - for simplicity, skip
                buffer.get() // Read the second byte of pointer
                break
            }

            if (length > 63 || !buffer.hasRemaining()) {
                // Invalid label length
                break
            }

            // Read the label
            val labelBytes = ByteArray(length)
            buffer.get(labelBytes)
            parts.add(String(labelBytes, Charsets.US_ASCII))

            iterations++
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(".")
        } else {
            ""
        }
    }

    /**
     * Check if a packet appears to be a DNS query (UDP to port 53).
     * This checks the IP header for UDP protocol and destination port 53.
     */
    fun isDnsQueryPacket(ipPacket: ByteArray): Boolean {
        if (ipPacket.size < 28) return false // Minimum IP + UDP header

        // Check IP version (should be 4 for IPv4)
        val version = (ipPacket[0].toInt() and 0xF0) shr 4
        if (version != 4) return false

        // IP header length (in 32-bit words)
        val ihl = (ipPacket[0].toInt() and 0x0F) * 4
        if (ipPacket.size < ihl + 8) return false // Need at least UDP header

        // Check protocol (should be 17 for UDP)
        val protocol = ipPacket[9].toInt() and 0xFF
        if (protocol != 17) return false

        // Get destination port from UDP header
        val destPort = ((ipPacket[ihl + 2].toInt() and 0xFF) shl 8) or (ipPacket[ihl + 3].toInt() and 0xFF)

        return destPort == 53
    }

    /**
     * Extract the DNS payload from an IP packet.
     * Returns the DNS data portion (after IP and UDP headers).
     */
    fun extractDnsPayload(ipPacket: ByteArray): ByteArray? {
        if (ipPacket.size < 28) return null

        // IP header length (in 32-bit words)
        val ihl = (ipPacket[0].toInt() and 0x0F) * 4

        // UDP header is 8 bytes, DNS data starts after
        val dnsStart = ihl + 8
        if (ipPacket.size <= dnsStart) return null

        return ipPacket.copyOfRange(dnsStart, ipPacket.size)
    }
}
