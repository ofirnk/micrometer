/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument;

import io.micrometer.core.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

public interface FunctionTimer extends Meter {
    static <T> Builder<T> builder(String name, T obj, ToLongFunction<T> countFunction,
                                  ToDoubleFunction<T> totalTimeFunction,
                                  TimeUnit totalTimeFunctionUnits) {
        return new Builder<>(name, obj, countFunction, totalTimeFunction, totalTimeFunctionUnits);
    }

    /**
     * The total number of occurrences of the timed event.
     */
    double count();

    /**
     * The total time of all occurrences of the timed event.
     */
    double totalTime(TimeUnit unit);

    default double mean(TimeUnit unit) {
        return count() == 0 ? 0 : totalTime(unit) / count();
    }

    TimeUnit baseTimeUnit();

    @Override
    default Iterable<Measurement> measure() {
        return Arrays.asList(
            new Measurement(this::count, Statistic.COUNT),
            new Measurement(() -> totalTime(baseTimeUnit()), Statistic.TOTAL_TIME)
        );
    }

    /**
     * Fluent builder for function timer.
     *
     * @param <T> The type of the state object from which the timer values are extracted.
     */
    class Builder<T> {
        private final String name;
        private final ToLongFunction<T> countFunction;
        private final ToDoubleFunction<T> totalTimeFunction;
        private final TimeUnit totalTimeFunctionUnits;
        private final List<Tag> tags = new ArrayList<>();

        @Nullable
        private final T obj;

        @Nullable
        private String description;

        private Builder(String name, @Nullable T obj,
                        ToLongFunction<T> countFunction,
                        ToDoubleFunction<T> totalTimeFunction,
                        TimeUnit totalTimeFunctionUnits) {
            this.name = name;
            this.obj = obj;
            this.countFunction = countFunction;
            this.totalTimeFunction = totalTimeFunction;
            this.totalTimeFunctionUnits = totalTimeFunctionUnits;
        }

        /**
         * @param tags Must be an even number of arguments representing key/value pairs of tags.
         */
        public Builder<T> tags(String... tags) {
            return tags(Tags.of(tags));
        }

        /**
         * @param tags Tags to add to the eventual meter.
         * @return The function timer builder with added tags.
         */
        public Builder<T> tags(Iterable<Tag> tags) {
            tags.forEach(this.tags::add);
            return this;
        }

        /**
         * @param key   The tag key.
         * @param value The tag value.
         * @return The function timer builder with a single added tag.
         */
        public Builder<T> tag(String key, String value) {
            tags.add(Tag.of(key, value));
            return this;
        }

        /**
         * @param description Description text of the eventual function timer.
         * @return The function timer builder with added description.
         */
        public Builder<T> description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Add the function timer to a single registry, or return an existing function timer in that registry. The returned
         * function timer will be unique for each registry, but each registry is guaranteed to only create one function timer
         * for the same combination of name and tags.
         *
         * @param registry A registry to add the function timer to, if it doesn't already exist.
         * @return A new or existing function timer.
         */
        public FunctionTimer register(MeterRegistry registry) {
            return registry.more().timer(new Meter.Id(name, tags, null, description, Type.TIMER), obj, countFunction, totalTimeFunction,
                totalTimeFunctionUnits);
        }
    }
}
