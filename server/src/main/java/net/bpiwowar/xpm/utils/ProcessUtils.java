package net.bpiwowar.xpm.utils;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessUtils {

    static OSType osType;
    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0)
            osType = OSType.WINDOWS;
        else if (os.indexOf("mac") >= 0)
            osType = OSType.MAC;
        else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0)
            osType = OSType.UNIX;

    }

    public static boolean isRunning(java.lang.Process process) {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /**
     * Get the PID of a process
     *
     * @param process
     * @return
     */
    public static int getPID(java.lang.Process process) {
        Field field;
        try {
            // Won't work on Windows
            field = process.getClass().getDeclaredField(osType == OSType.WINDOWS ? "handle" : "pid");
            field.setAccessible(true);
            int pid = field.getInt(process);
            field.setAccessible(false);
            return pid;
        } catch (SecurityException e) {
            throw new RuntimeException("Could not get the XPMProcess of the process",
                    e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not get the XPMProcess of the process",
                    e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Could not get the XPMProcess of the process",
                    e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not get the XPMProcess of the process",
                    e);
        }
    }

    /**
     * Get the PID of the java runtime
     *
     * @return
     */
    public static int getPID() {
        RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
        String processName = rtb.getName();
        return tryPattern1(processName);
    }

    private static Integer tryPattern1(String processName) {
        Integer result = null;

        /* tested on: */
        /* - windows xp sp 2, java 1.5.0_13 */
        /* - mac os x 10.4.10, java 1.5.0 */
        /* - debian linux, java 1.5.0_13 */
        /* all return pid@host, e.g 2204@antonius */

        Pattern pattern = Pattern.compile("^([0-9]+)@.+$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(processName);
        if (matcher.matches()) {
            result = Integer.parseInt(matcher.group(1));
        }
        return result;

    }

    enum OSType {
        WINDOWS, UNIX, MAC
    }
}
