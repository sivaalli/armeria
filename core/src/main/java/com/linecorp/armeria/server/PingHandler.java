/*
 * Copyright 2020 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Exception;

interface PingHandler {

    void start(ChannelHandlerContext ctx);

    /**
     * Handles when a {@code PING} is received with {@code ACK} flag set.
     */
    void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception;

    /**
     * Handles when a {@code PING} is received with {@code ACK} not set. Typically
     * received from client.
     */
    void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception;
}
