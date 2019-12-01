/*
 * Copyright 2019 LINE Corporation
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

package com.linecorp.armeria.server.thrift;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;

import com.linecorp.armeria.common.SerializationFormat;
import com.linecorp.armeria.common.thrift.ThriftSerializationFormats;
import com.linecorp.armeria.server.RpcService;

/**
 * A fluent builder to build an instance of {@link THttpService}. This builder allows to bind multiple thrift
 * service implementations along with mixing TMultiplexed protocol to a route.
 * <h2>Example</h2>
 * <pre>{@code
 * final ServerBuilder serverBuilder = Server.builder();
 * sb.port(8080, SessionProtocol.HTTP)
 *    .service("/", THttpService.builder()
 *                               .addService(new FooService())              // foo() method
 *                               .addService(new BarService())              // bar() method
 *                               .addService("foobar", new FooBarService()) // TMultiplexed service
 *                               .build())
 *           .build();
 * }</pre>
 *
 * <p>When the thrift request has a method {@code foo()} then {@code FooService.foo()} handles the request and
 * similarly when thrift request has a method {@code bar()} then {@code BarService.bar()} handles the request.
 * And when the service name is "foobar" then FooBarService</p>
 *
 * @see THttpService
 * @see ThriftCallService
 */
public final class THttpServiceBuilder {

    private static final SerializationFormat[] EMPTY_FORMATS = new SerializationFormat[0];
    private final ImmutableListMultimap.Builder<String, Object> implementationsBuilder =
            ImmutableListMultimap.builder();
    private SerializationFormat defaultSerializationFormat = ThriftSerializationFormats.BINARY;
    private Set<SerializationFormat> otherSerializationFormats = ThriftSerializationFormats.values();
    @Nullable
    private Function<? super RpcService, ? extends RpcService> decoratorFunction;

    THttpServiceBuilder() { }

    private static SerializationFormat[] newAllowedSerializationFormats(
            SerializationFormat defaultSerializationFormat,
            Iterable<SerializationFormat> otherAllowedSerializationFormats) {

        requireNonNull(defaultSerializationFormat, "defaultSerializationFormat");
        requireNonNull(otherAllowedSerializationFormats, "otherAllowedSerializationFormats");

        final Set<SerializationFormat> set = new LinkedHashSet<>();
        set.add(defaultSerializationFormat);
        Iterables.addAll(set, otherAllowedSerializationFormats);
        return set.toArray(EMPTY_FORMATS);
    }

    /**
     * Adds a new {@code TMultiplexed} service to the builder.
     *
     * @param name name of the service.
     * @param implementation an implementation of {@code *.Iface} or {@code *.AsyncIface} service interface
     *                       generated by the Apache Thrift compiler.
     */
    public THttpServiceBuilder addService(String name, Object implementation) {
        implementationsBuilder.put(name, implementation);
        return this;
    }

    /**
     * Add a new service implementation to the builder.
     *
     * @param implementation an implementation of {@code *.Iface} or {@code *.AsyncIface} service interface
     *                       generated by the Apache Thrift compiler
     */
    public THttpServiceBuilder addService(Object implementation) {
        implementationsBuilder.put("", implementation);
        return this;
    }

    /**
     * Adds other {@link SerializationFormat} to the builder. Current supported {@link SerializationFormat}s are
     * {@link ThriftSerializationFormats#values()}. If nothing is specified then all the
     * {@link SerializationFormat#values()}s are added.
     *
     * <p>Currently, the only way to specify a serialization format is by using the HTTP session
     * protocol and setting the Content-Type header to the appropriate {@link SerializationFormat#mediaType()}.
     */
    public THttpServiceBuilder otherSerializationFormats(SerializationFormat... otherSerializationFormats) {
        requireNonNull(otherSerializationFormats, "otherSerializationFormats");
        this.otherSerializationFormats = new LinkedHashSet<>();
        this.otherSerializationFormats.addAll(Arrays.asList(otherSerializationFormats));
        return this;
    }

    /**
     * Adds the default serialization format which will be used when the client does not specify one in
     * request.
     *
     * <p>Currently, the only way to specify a serialization format is by using the HTTP session
     * protocol and setting the Content-Type header to the appropriate {@link SerializationFormat#mediaType()}.
     */
    public THttpServiceBuilder defaultSerializationFormat(SerializationFormat defaultSerializationFormat) {
        requireNonNull(defaultSerializationFormat, "defaultSerializationFormat");
        this.defaultSerializationFormat = defaultSerializationFormat;
        return this;
    }

    /**
     * A {@code Function<? super RpcService, ? extends RpcService>} to decorate the {@link RpcService}.
     */
    public THttpServiceBuilder decorate(Function<? super RpcService, ? extends RpcService> decoratorFunction) {
        requireNonNull(decoratorFunction, "decoratorFunction");
        if (this.decoratorFunction == null) {
            this.decoratorFunction = decoratorFunction;
        } else {
            this.decoratorFunction = this.decoratorFunction.andThen(decoratorFunction);
        }
        return this;
    }

    private RpcService decorate(RpcService service) {
        if (decoratorFunction != null) {
            return service.decorate(decoratorFunction);
        }
        return service;
    }

    /**
     * Builds a new instance of {@link THttpService}.
     */
    public THttpService build() {
        @SuppressWarnings("UnstableApiUsage")
        final Map<String, List<Object>> implementations = Multimaps.asMap(implementationsBuilder.build());
        final ThriftCallService tcs = ThriftCallService.of(implementations);

        final LinkedHashSet<SerializationFormat> combined = new LinkedHashSet<>();
        combined.add(defaultSerializationFormat);
        combined.addAll(otherSerializationFormats);

        return new THttpService(decorate(tcs), newAllowedSerializationFormats(defaultSerializationFormat,
                                                                              otherSerializationFormats));
    }
}
