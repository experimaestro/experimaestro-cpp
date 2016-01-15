package net.bpiwowar.xpm.scheduler;

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

import net.bpiwowar.xpm.commands.Command;
import net.bpiwowar.xpm.commands.CommandOutput;
import net.bpiwowar.xpm.commands.CommandPath;
import net.bpiwowar.xpm.commands.Commands;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.commands.XPMScriptProcessBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import net.bpiwowar.xpm.connectors.*;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.utils.TemporaryDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests on command
 */
public class CommandTest  {

    @Test(description = "CommandOutput handling")
    public void commandOutputTest() throws IOException, InterruptedException, LaunchException {
        try(TemporaryDirectory directory = new TemporaryDirectory("commandtest", "test")) {
            directory.setAutomaticDelete(false);

            final File dataFile = new File(directory.getFile(), "data");
            try(FileWriter out = new FileWriter (dataFile)) {
                out.write("hello\n");
                out.write("world\n");
            }
            final Commands commands = new Commands();
            final Command command = new Command();

            final LocalhostConnector connector = Scheduler.get().getLocalhostConnector();
            final Launcher launcher = new DirectLauncher(connector);
            final Path dataPath = connector.resolve(dataFile.getAbsolutePath());


            final Command subCommand = new Command();
            subCommand.add("/bin/cat", new CommandPath(dataPath));
            final CommandOutput output = subCommand.output();

            command.add("/usr/bin/paste", output, output);
            commands.add(command);


            final Path locatorPath = new File(directory.getFile(), "task").toPath();
//            ResourceLocator locator = new ResourceLocator(launcher, locatorPath);
            final Path runFile = Resource.RUN_EXTENSION.transform(locatorPath);

            XPMScriptProcessBuilder builder = launcher.scriptProcessBuilder(runFile, null);
            builder.directory(directory.getFile().toPath());

            // Add command
            builder.command(commands);

            builder.redirectInput(Redirect.INHERIT);
            final Path out = connector.resolveFile(new File(directory.getFile(), "output").getAbsolutePath());
            builder.redirectOutput(Redirect.to(out));
            builder.redirectError(Redirect.INHERIT);

            final XPMProcess process = builder.start();
            int code = process.waitFor();
            Assert.assertEquals(code, 0, "Exit code is not 0");

            // Checks the output

            try(BufferedReader in = Files.newBufferedReader(out)) {
                Assert.assertEquals(in.readLine(), "hello\thello");
                Assert.assertEquals(in.readLine(), "world\tworld");
            }

        }
    }
}
