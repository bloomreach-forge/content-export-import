/*
 * Copyright 2024 Bloomreach B.V. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.content.exim.core.util;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.ext.LoggerWrapper;

/**
 * Tee-ing LoggerWrapper, wrapping a logger while tee-ing to the {@code second} logger.
 */
public class TeeLoggerWrapper extends LoggerWrapper {

    private Logger second;

    /**
     * Wrapping the {@code logger} while tee-ing to the {@code second} logger.
     * @param logger primary logger
     * @param second secondary logger
     */
    public TeeLoggerWrapper(Logger logger, Logger second) {
        super(logger, TeeLoggerWrapper.class.getName());

        if (second == null) {
            throw new IllegalArgumentException("Second logger is null!");
        }

        this.second = second;
    }

    @Override
    public void trace(String msg) {
        super.trace(msg);
        second.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        super.trace(format, arg);
        second.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        super.trace(format, arg1, arg2);
        second.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        super.trace(format, arguments);
        second.trace(format, arguments);
    }

    public void trace(String msg, Throwable t) {
        super.trace(msg, t);
        second.trace(msg, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        super.trace(marker, msg);
        second.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        super.trace(marker, format, arg);
        second.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        super.trace(marker, format, arg1, arg2);
        second.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        super.trace(marker, format, argArray);
        second.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        super.trace(marker, msg, t);
        second.trace(marker, msg, t);
    }

    @Override
    public void debug(String msg) {
        super.debug(msg);
        second.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        super.debug(format, arg);
        second.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        super.debug(format, arg1, arg2);
        second.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        super.debug(format, arguments);
        second.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        super.debug(msg, t);
        second.debug(msg, t);
    }

    @Override
    public void debug(Marker marker, String msg) {
        super.debug(marker, msg);
        second.debug(marker, msg);
    }

    public void debug(Marker marker, String format, Object arg) {
        super.debug(marker, format, arg);
        second.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        super.debug(marker, format, arg1, arg2);
        second.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        super.debug(marker, format, arguments);
        second.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        super.debug(marker, msg, t);
        second.debug(marker, msg, t);
    }

    @Override
    public void info(String msg) {
        super.info(msg);
        second.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        super.info(format, arg);
        second.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        super.info(format, arg1, arg2);
        second.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        super.info(format, arguments);
        second.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        super.info(msg, t);
        second.info(msg, t);
    }

    @Override
    public void info(Marker marker, String msg) {
        super.info(marker, msg);
        second.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        super.info(marker, format, arg);
        second.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        super.info(marker, format, arg1, arg2);
        second.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        super.info(marker, format, arguments);
        second.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        super.info(marker, msg, t);
        second.info(marker, msg, t);
    }

    @Override
    public void warn(String msg) {
        super.warn(msg);
        second.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        super.warn(format, arg);
        second.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        super.warn(format, arguments);
        second.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        super.warn(format, arg1, arg2);
        second.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        super.warn(msg, t);
        second.warn(msg, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        super.warn(marker, msg);
        second.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        super.warn(marker, format, arg);
        second.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        super.warn(marker, format, arg1, arg2);
        second.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        super.warn(marker, format, arguments);
        second.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        super.warn(marker, msg, t);
        second.warn(marker, msg, t);
    }

    @Override
    public void error(String msg) {
        super.error(msg);
        second.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        super.error(format, arg);
        second.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        super.error(format, arg1, arg2);
        second.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        super.error(format, arguments);
        second.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        super.error(msg, t);
        second.error(msg, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        super.error(marker, msg);
        second.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        super.error(marker, format, arg);
        second.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        super.error(marker, format, arg1, arg2);
        second.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        super.error(marker, format, arguments);
        second.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        super.error(marker, msg, t);
        second.error(marker, msg, t);
    }

}
